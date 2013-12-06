package floobits;

abstract class RunLater {
    Object data;
    abstract void run(Object... objects);

    protected RunLater(Object data) {
        this.data = data;
    }
}
