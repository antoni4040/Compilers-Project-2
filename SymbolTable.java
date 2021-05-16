import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class SymbolTable {
    public Map<String, ClassSymbolTable> table;
    ClassSymbolTable current = null;

    public int currentIndex = 0; // It's a suprise tool that will help us later.
    public List<String> params;

    public SymbolTable() {
        this.table = new HashMap<String, ClassSymbolTable>();
    }

    public void enter(ClassSymbolTable toEnter) {
        this.current = toEnter;
    }

    public void exit() {
        this.current = null;
    }

    public ClassSymbolTable getCurrent() {
        return this.current;
    }

    public void printTable() {
        System.out.println("Symbol table:");
        for (Map.Entry<String, ClassSymbolTable> entry : this.table.entrySet()) {
            System.out.println("Key = " + entry.getKey());
            entry.getValue().printClass();
        }
    }

    public void printTableInfo() {
        for (Map.Entry<String, ClassSymbolTable> entry : this.table.entrySet()) {
            System.out.println("-----------" + "Class " + entry.getKey() + "-----------");
            entry.getValue().printClassInfo(this.table);
            System.out.println();
        }
    }
}
