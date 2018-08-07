package edu.buffalo.cse.cse486586.groupmessenger2;

//References
//Priority Queue: https://docs.oracle.com/javase/7/docs/api/java/util/PriorityQueue.html
//Exceptions: https://docs.oracle.com/javase/7/docs/api/java/lang/Exception.html

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.TreeMap;


import static android.content.ContentValues.TAG;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String REMOTE_PORTS[] = {"11108", "11112", "11116", "11120", "11124"};
    static HashMap<String, String> portNames = new HashMap<String, String>(); //naming all the ports
    static HashMap<String, Integer> procNames = new HashMap<String, Integer>();
    static final int SERVER_PORT = 10000;
    public static final String KEY = "key";
    public static final String VALUE = "value";
    EditText editText;

    //Re-using from PA2A
    private final Uri CONTENT_URI = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
    private ContentResolver contentResolver;
    ContentValues cv = new ContentValues(2);

    //For part 1
    static int _code = 0; //Key value for the message for storing in DB
    static int msgID = 0; //Message ID that will be stored for each message
    static int seqNum = 0; //Sequence number that will be determined for the message to be sent
    int propSeqNum = 0; //Sequence number that will be proposed for the incoming messages
    PriorityQueue<Message> messageQ = new PriorityQueue<Message>();
    //to store the messages recieved based on their sequence no and deliver
    //array to hold all the received proposed sequence numbers
    TreeMap<Double, String> deliveryQ =  new TreeMap<Double, String>();
    Double seqArr[] = new Double[REMOTE_PORTS.length];


    //For part 2
    static int FAILED_PROCESS = 0;

    //Telephony Hack
    TelephonyManager tel;
    String portStr;
    String myPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        //Anirban:start
        contentResolver = getContentResolver();
        final String messages = "";
        tv.setText(messages);

        //Naming all the ports
        portNames.put("11108", "A");
        portNames.put("11112", "B");
        portNames.put("11116", "C");
        portNames.put("11120", "D");
        portNames.put("11124", "E");

        procNames.put("A", 1);
        procNames.put("B", 2);
        procNames.put("C", 3);
        procNames.put("D", 4);
        procNames.put("E", 5);
        //Anirban:end
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        //Anirban:start
        tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        editText = (EditText) findViewById(R.id.editText1);
        Button sendButton = (Button) findViewById(R.id.button4);

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        //from PA2A
        sendButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                String msg = editText.getText().toString();
                if (msg!=null && !msg.equals("")) {
                    editText.setText("");

                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                }
            }
        });
        //Anirban:end
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    //Anirban:start
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            String input;
            while (true) {
                try {
                    Socket server = serverSocket.accept();
                    BufferedReader serverInput = new BufferedReader(new InputStreamReader(server.getInputStream()));
                    if ((input = serverInput.readLine()) != null) {
                        String values[] = input.split(":");
                        if (values[2].split("-")[0].equals("PS")){
                            propSeqNum++;
                            Message msg = new Message(values[1], values[3]);
                            msg.setProcessId(values[0]);
                            msg.setIsDeliverable(false);
                            msg.setAgreedSequenceId(Double.parseDouble(String.valueOf(propSeqNum+(0.1*Double.parseDouble(msg.getProcessId())))));
                            messageQ.add(msg);

                            //creating and sending sending proposal
                            String proposedMsg = String.valueOf(procNames.get(portNames.get(myPort)))+":"+msg.getMessageId()+":"
                                    +"PS-"+String.valueOf(msg.getAgreedSequenceId())+"-"+String.valueOf(msg.isDeliverable())+":Proposal Message";
                            PrintWriter client = new PrintWriter(server.getOutputStream(), true);
                            client.println(proposedMsg);
                        }
                        else if (values[2].split("-")[0].equals("AS")){
                            Message msg = new Message(values[1], values[3]);
                            msg.setProcessId(values[0]);
                            msg.setIsDeliverable(true);
                            msg.setAgreedSequenceId(Double.parseDouble(values[2].split("-")[1]));

                            //the sequence number should be maximum of all it has seen so far
                            Double mx = Math.max(msg.getAgreedSequenceId().intValue(), Math.ceil(propSeqNum));
                            propSeqNum = mx.intValue() + 1;

                            //removing the message based on message id
                            Iterator<Message> i = messageQ.iterator();
                            while(i.hasNext()){
                                Message t = i.next();
                                if (t.getMessageId().equals(values[1])){
                                    messageQ.remove(t);
                                }
                            }

                            //and adding it again with the new sequence number
                            messageQ.offer(msg);

                            //publishing while the messages are deliverable
                            while (!messageQ.isEmpty()){
                                if (messageQ.peek().isDeliverable()) {
                                    deliveryQ.put(messageQ.peek().getAgreedSequenceId(), messageQ.peek().getMessageBody());
                                    for (Double d: deliveryQ.keySet()){
                                        publishProgress(deliveryQ.get(d));
                                    }
                                    deliveryQ.remove(messageQ.peek().getAgreedSequenceId());
                                }

                                //after publishing removing it from the message queue
                                messageQ.poll();
                            }
                        }
                        else if (values[2].split("-")[0].equals("FN")){
                            String pID = values[0];
                            FAILED_PROCESS = FAILED_PROCESS == Integer.parseInt(pID) ? FAILED_PROCESS : Integer.parseInt(pID);
                            //removing the message based on message id
                            Iterator<Message> i = messageQ.iterator();
                            while(i.hasNext()){
                                Message t = i.next();
                                if (t.getProcessId().equals(pID)){
                                    messageQ.remove(t);
                                }
                            }
                            while (!messageQ.isEmpty()){
                                if (messageQ.peek().isDeliverable()) {
                                    deliveryQ.put(messageQ.peek().getAgreedSequenceId(), messageQ.peek().getMessageBody());
                                    for (Double d: deliveryQ.keySet()){
                                        publishProgress(deliveryQ.get(d));
                                    }
                                    deliveryQ.remove(messageQ.peek().getAgreedSequenceId());
                                }
                                //after publishing removing it from the message queue
                                messageQ.poll();
                            }
                        }
                    }
                    server.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException");
                }
            }
        }

        protected void onProgressUpdate(String...strings) {
            String strReceived = strings[0].trim();
            TextView textView = (TextView) findViewById(R.id.textView1);
            textView.append(strReceived + "\t\n");

            //From PA2A
            cv.put(KEY, String.valueOf(_code++));
            cv.put(VALUE, strReceived);
            contentResolver.insert(CONTENT_URI, cv);

            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            try {
                //Anirban: start
                //Storing the message as a new message object
                String msg = strings[0];
                Message message = new Message(String.valueOf(msgID++)+portNames.get(myPort), msg);
                message.setAgreedSequenceId(Double.parseDouble(String.valueOf(seqNum++)));
                message.setProcessId(portNames.get(myPort));
                String msgToSend = message.getMessageBody();
                String msgId = message.getMessageId();
                String processId = message.getProcessId();

                try{
                    //first iteration for sending out proposals
                    for (int i = 0; i < REMOTE_PORTS.length; i++) {
                        String remotePort = REMOTE_PORTS[i];
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePort));
                        //sending message with proposed sequence number
                        try {
                            PrintWriter client = new PrintWriter(socket.getOutputStream(), true);
                            String temp = procNames.get(processId)+":"+msgId+":"+"PS-"+String.valueOf(message.getAgreedSequenceId())+"-"+String.valueOf(message.isDeliverable())+":"+msgToSend;
                            client.println(temp);
                            //getting back responses from other clients and storing them
                            BufferedReader serverReceived = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            String received = serverReceived.readLine();
                            if (received != null){
                                seqArr[i] = Double.parseDouble(received.split(":")[2].split("-")[1]);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (SocketException s){
                    FAILED_PROCESS = procNames.get(portNames.get(myPort));
                    notifyAVD(msgId);
                    Log.e("SocketException", "Process "+FAILED_PROCESS+" failed due to SocketException while sending messages for the first time");
                } catch (IOException io){
                    FAILED_PROCESS = procNames.get(portNames.get(myPort));
                    notifyAVD(msgId);
                    Log.e("IOException", "Process "+FAILED_PROCESS+" failed due to IOException while sending messages for the first time");
                } catch (Exception e){
                    FAILED_PROCESS = procNames.get(portNames.get(myPort));
                    notifyAVD(msgId);
                    Log.e("Exception", "Process "+FAILED_PROCESS+" failed due to Exception while sending messages for the first time");
                }

                //finding out the maximum sequence number from all the received proposals
                Double max = 0.0;
                for (Double j:seqArr){
                    max = max>j?max:j;
                }
                message.setAgreedSequenceId(max);
                message.setIsDeliverable(true);

                //second iteration for sending out finalised sequence
                try{
                    for (int i = 0; i < REMOTE_PORTS.length; i++) {
                        if (FAILED_PROCESS != 0){
                            if (procNames.get(portNames.get(myPort)) == FAILED_PROCESS)
                                continue;
                        }
                        String remotePort = REMOTE_PORTS[i];
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePort));
                        //sending message with proposed sequence number
                        try {
                            PrintWriter client = new PrintWriter(socket.getOutputStream(), true);
                            String temp = procNames.get(processId)+":"+message.getMessageId()
                                    +":"+"AS-"+String.valueOf(message.getAgreedSequenceId())+"-"+String.valueOf(message.isDeliverable())
                                    +":"+message.getMessageBody();
                            client.println(temp);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (SocketException s){
                    FAILED_PROCESS = procNames.get(portNames.get(myPort));
                    notifyAVD(msgId);
                    Log.e("SocketException", "Process "+FAILED_PROCESS+" failed due to SocketException while sending messages for the second time");
                } catch (IOException io){
                    FAILED_PROCESS = procNames.get(portNames.get(myPort));
                    notifyAVD(msgId);
                    Log.e("SocketException", "Process "+FAILED_PROCESS+" failed due to IOException while sending messages for the second time");
                } catch (Exception e){
                    FAILED_PROCESS = procNames.get(portNames.get(myPort));
                    notifyAVD(msgId);
                    Log.e("SocketException", "Process "+FAILED_PROCESS+" failed due to Exception while sending messages for the second time");
                }

                //Anirban: end

            }
            finally {
                return null;
            }
        }
    }

    public static void notifyAVD (String msgId){
        try{
            //sending out message to all other AVDs
            for (int i=0; i< REMOTE_PORTS.length; i++){
                String remotePort = REMOTE_PORTS[i];
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(remotePort));
                PrintWriter client = new PrintWriter(socket.getOutputStream(), true);
                String temp = String.valueOf(FAILED_PROCESS)+":"+msgId+":"+"FN-"+String.valueOf(FAILED_PROCESS);
                client.println(temp);
            }
        } catch (UnknownHostException e) {
            Log.e(TAG, "ClientTask UnknownHostException");
        }   catch (IOException e) {
            Log.e(TAG, "ClientTask socket IOException");
        }
    }
}
