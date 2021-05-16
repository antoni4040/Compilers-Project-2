public class methodSymbolTable {
    public Map<String, String> params;
    public Map<String, String> vars;
    public String type;

    public methodSymbolTable(String type_) {
        this.params = new LinkedHashMap<String, String>();
        this.vars = new LinkedHashMap<String, String>();
        this.type = type_;
    }

    public void printMethod() {
        System.out.println("Params");
        for (Map.Entry<String, String> entry : this.params.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
        System.out.println("Vars");
        for (Map.Entry<String, String> entry : this.vars.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
    }
}