import java.io.File;

public class FileRequestMessage extends Message{
    private File path;

    public File getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = new File(path);
    }
    public void setPath(File path) {
        this.path = path;
    }
}
