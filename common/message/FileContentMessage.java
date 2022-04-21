public class FileContentMessage extends Message {
    private long startPosition;
    private byte[] content;
    private boolean last;

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public void setStartPosition(long startPosition) {
        this.startPosition = startPosition;
    }

    public long getStartPosition() {
        return startPosition;
    }

    public byte[] getContent() {
        return content;
    }
    public void setContent(byte[] content) {
        this.content = content;
    }
}
