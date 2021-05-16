public class classSymbolTable {
    public Map<String, String> fields;
    public Map<String, methodSymbolTable> methods;
    public String parent;
    public String name;
    methodSymbolTable current = null;

    public classSymbolTable(String name_, String parent_) {
        this.fields = new LinkedHashMap<String, String>();
        this.methods = new LinkedHashMap<String, methodSymbolTable>();
        this.name = name_;
        this.parent = parent_;
    }

    public void enter(methodSymbolTable toEnter) {
        this.current = toEnter;
    }

    public void exit() {
        this.current = null;
    }

    public methodSymbolTable getCurrent() {
        return this.current;
    }

    public void printClass() {
        System.out.println("Fields");
        for (Map.Entry<String, String> entry : this.fields.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
        System.out.println("Methods");
        for (Map.Entry<String, methodSymbolTable> entry : this.methods.entrySet()) {
            System.out.println(entry.getKey());
            entry.getValue().printMethod();
        }
    }

    public int classFieldsOffset() {
        int position = 0;
        for (Map.Entry<String, String> entry : this.fields.entrySet()) {
            if(entry.getValue().equals("int")) {
                position += 4;
            }
            else if(entry.getValue().equals("boolean")) {
                position += 1;
            }
            else {
                position += 8;
            }
        }
        return position;
    }

    public int classMethodsOffset() {
        int position = 0;
        for (Map.Entry<String, methodSymbolTable> entry : this.methods.entrySet()) {
            position += 8;
        }
        return position;
    }

    public void printClassInfo(Map<String, classSymbolTable> table) {
        int position = 0;

        if(this.parent != null) {
            classSymbolTable parentClass = table.get(this.parent);
            position = parentClass.classFieldsOffset();
        }

        System.out.println("--Variables---");
        for (Map.Entry<String, String> entry : this.fields.entrySet()) {
            System.out.println(this.name + "." + entry.getKey() + ":" + position);
            if(entry.getValue().equals("int")) {
                position += 4;
            }
            else if(entry.getValue().equals("boolean")) {
                position += 1;
            }
            else {
                position += 8;
            }
        }

        position = 0;
        if(this.parent != null) {
            classSymbolTable parentClass = table.get(this.parent);
            position = parentClass.classMethodsOffset();
        }

        System.out.println("---Methods---");
        for (Map.Entry<String, methodSymbolTable> entry : this.methods.entrySet()) {
            System.out.println(this.name + "." + entry.getKey() + ":" + position);
            position += 8;
        }
    }
}