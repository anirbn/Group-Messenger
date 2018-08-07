package edu.buffalo.cse.cse486586.groupmessenger2;

/**
 * Created by anirban on 3/4/18.
 */

//References
// Comparable: https://docs.oracle.com/javase/8/docs/api/java/lang/Comparable.html


public class Message implements Comparable<Message>{
    private String processId;
    private String messageId;
    private String messageBody;
    private Double agreedSequenceId;
    private boolean isDeliverable;

    Message(String messageId, String messageBody){
        this.messageId = messageId;
        this.messageBody = messageBody;
        //this.proposedSequenceId = proposedSequenceId;
        agreedSequenceId = 0.0;
        isDeliverable = false;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public Double getAgreedSequenceId() {
        return agreedSequenceId;
    }

    public void setAgreedSequenceId(Double agreedSequemceId) {
        this.agreedSequenceId = agreedSequemceId;
    }

    public boolean isDeliverable() {
        return isDeliverable;
    }

    public void setIsDeliverable(boolean deliverable) {
        isDeliverable = deliverable;
    }

    @Override
    public String toString(){
        return this.getProcessId()+"|"+this.getMessageId()+"|"+this.getAgreedSequenceId()+"|"+this.messageBody;
    }


    public int compareTo(Message other) {
        if (this.agreedSequenceId < other.getAgreedSequenceId()){
            return -1;
        }
        else if (this.agreedSequenceId > other.getAgreedSequenceId()){
            return 1;
        }
        else {
            return 0;
        }
    }
}
