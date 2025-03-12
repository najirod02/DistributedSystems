package it.unitn.ds1;
import akka.actor.ActorRef;
import akka.actor.AbstractActor;
import java.util.Random;
import java.io.Serializable;
import akka.actor.Props;
import java.util.List;
import java.util.ArrayList;
import java.lang.Thread;
import java.lang.InterruptedException;
import java.util.Collections;

class Chatter extends AbstractActor {
  
  // number of chat messages to send
  final static int N_MESSAGES = 5;
  private Random rnd = new Random();
  private List<ActorRef> group; // the list of peers (the multicast group)
  private int sendCount = 0;    // number of sent messages
  private String myTopic;       // The topic I am interested in, null if no topic
  private final int id;         // ID of the current actor
  private int[] vc;             // the local vector clock

  // a buffer storing all received chat messages
  private StringBuffer chatHistory = new StringBuffer();

  // TODO 2: provide a buffer for out-of-order messages
  private List<ChatMsg> chatOutOfOrder = new ArrayList<>();

  /* -- Message types ------------------------------------------------------- */
  
  // Start message that informs every chat participant about its peers
  public static class JoinGroupMsg implements Serializable {
    private final List<ActorRef> group; // list of group members
    public JoinGroupMsg(List<ActorRef> group) {
      this.group = Collections.unmodifiableList(new ArrayList<>(group));
    }
  }

  // A message requesting the peer to start a discussion on his topic
  public static class StartChatMsg implements Serializable {}

  // Chat message
  public static class ChatMsg implements Serializable {
    public final String topic;   // "topic" of the conversation
    public final int n;          // the number of the reply in the current topic
    public final int senderId;   // the ID of the message sender
    public final int[] vc;       // vector clock

    public ChatMsg(String topic, int n, int senderId, int[] vc) {
      this.topic = topic;
      this.n = n;
      this.senderId = senderId;
      this.vc = new int[vc.length];
      for (int i=0; i<vc.length; i++) this.vc[i] = vc[i];
    }
  }

  // A message requesting to print the chat history
  public static class PrintHistoryMsg implements Serializable {}

  /* -- Actor constructor --------------------------------------------------- */

  public Chatter(int id, String topic) {
    this.id = id;
    this.myTopic = topic;
  }

  static public Props props(int id, String topic) {
    return Props.create(Chatter.class, () -> new Chatter(id, topic));
  }

  /* -- Actor behaviour ----------------------------------------------------- */

  private void sendChatMsg(String topic, int n) {
    sendCount++;

    // TODO 3: update vector clock
    this.vc[this.id]++;

    // generate chat message
    ChatMsg m = new ChatMsg(topic, n, this.id, this.vc);
    System.out.printf("%02d: %s%02d\n", this.id, topic, n);

    // send to peers and append to log
    multicast(m);
    appendToHistory(m);
  }

  private void multicast(Serializable m) {

    // randomly arrange peers
    List<ActorRef> shuffledGroup = new ArrayList<>(group);
    Collections.shuffle(shuffledGroup);

    // multicast to all peers in the group (do not send any message to self)
    for (ActorRef p: shuffledGroup) {
      if (!p.equals(getSelf())) {
        p.tell(m, getSelf());

        // simulate network delays using sleep
        try { Thread.sleep(rnd.nextInt(10)); } 
        catch (InterruptedException e) { e.printStackTrace(); }
      }
    }
  }

  /*
  private void multicast(Serializable m) {
    for (ActorRef p: group) {
      if (!p.equals(getSelf()))
      p.tell(m, getSelf());
    }
  }
  */

  // Here we define the mapping between the received message types
  // and our actor methods
  @Override
  public Receive createReceive() {
    return receiveBuilder()
      .match(JoinGroupMsg.class,    this::onJoinGroupMsg)
      .match(StartChatMsg.class,    this::onStartChatMsg)
      .match(ChatMsg.class,         this::onChatMsg)
      .match(PrintHistoryMsg.class, this::printHistory)
      .build();
  }

  private void onJoinGroupMsg(JoinGroupMsg msg) {
    this.group = msg.group;

    // create the vector clock
    this.vc = new int[this.group.size()];
    System.out.printf("%s: joining a group of %d peers with ID %02d and topic %s\n", 
        getSelf().path().name(), this.group.size(), this.id, this.myTopic);
  }

  private void onStartChatMsg(StartChatMsg msg) {

    // start topic with message 0
    sendChatMsg(myTopic, 0);
  }

  private void onChatMsg(ChatMsg msg) {

    // "deliver" the message to the simulated chat user
    //of course now we cannot deliver instantly as we need to check the vc firstly
    //deliver(msg);

    // TODO 4: deliver only if the message is in-order
    // TODO 4 hint: once a message is delivered, update the vector clock...
    /*
     * this strategy allows or total order among messages of the same topic meaning that 
     * if i receive two messages from two different topics then there is no defined and correct 
     * order so any delivering is correct.
     */

    if((this.vc[msg.senderId]+1) == msg.vc[msg.senderId]){
      //we can deliver the message
      //update vector clock
      for(int k=0; k<this.vc.length; k++){
        this.vc[k] = Math.max(this.vc[k], msg.vc[k]);
      }

      deliver(msg);

      //check if we can deliver previous received messages
      List<ChatMsg> toRemove = new ArrayList<>();
      for (ChatMsg bufferedMsg : chatOutOfOrder) {
          if (canDeliver(bufferedMsg)) {
              deliver(bufferedMsg);
              toRemove.add(bufferedMsg);
          }
      }

      // Remove the delivered messages
      chatOutOfOrder.removeAll(toRemove);
      return;
    }

    //cannot deliver the message
    //we need to save the message in the buffer
    chatOutOfOrder.add(msg);
  }

  /**
  * function to check if a buffered message can now be delivered.
  * it is important that the vc of the reciver has to be greater or equal
  * than the one of the sender otherwise it means that the agent don't know
  * extactly the global state
  */
  private boolean canDeliver(ChatMsg msg) {
    for (int i = 0; i < this.vc.length; i++) {
      if(i != msg.senderId){  
        if (msg.vc[i] >= this.vc[i]) {
              return false;
          }
      }
    }
    return true;
  }

  private void deliver(ChatMsg m) {

    // our "chat application" appends all the received messages to the
    // chatHistory and replies if the topic of the message is interesting
    appendToHistory(m);

    // if the message is on my topic and I still have something to say...
    if (m.topic.equals(myTopic) && sendCount < N_MESSAGES) {

      // reply to the received message with an incremented value and the same topic
      sendChatMsg(m.topic, m.n+1);  
    }
  }

  private void appendToHistory(ChatMsg m) {
    chatHistory.append(m.topic).append(m.n).append(" ");
  }

  private void printHistory(PrintHistoryMsg msg) {
    System.out.printf("%02d: %s\n", this.id, chatHistory);
  }
}
