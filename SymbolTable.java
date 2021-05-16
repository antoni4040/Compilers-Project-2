public class SymbolTable {
    public Map<String, classSymbolTable> table;
    classSymbolTable current = null;

    public int currentIndex = 0; // It's a suprise tool that will help us later.
    public List<String> params;

    public SymbolTable() {
        this.table = new HashMap<String, classSymbolTable>();
    }

    public void enter(classSymbolTable toEnter) {
        this.current = toEnter;
    }

    public void exit() {
        this.current = null;
    }

    public classSymbolTable getCurrent() {
        return this.current;
    }

    public void printTable() {
        System.out.println("Symbol table:");
        for (Map.Entry<String, classSymbolTable> entry : this.table.entrySet()) {
            System.out.println("Key = " + entry.getKey());
            entry.getValue().printClass();
        }
    }

    public void printTableInfo() {
        for (Map.Entry<String, classSymbolTable> entry : this.table.entrySet()) {
            System.out.println("-----------" + "Class " + entry.getKey() + "-----------");
            entry.getValue().printClassInfo(this.table);
            System.out.println();
        }
    }
}
