import syntaxtree.*;
import visitor.*;

public class DeclVisitor extends GJDepthFirst<String, SymbolTable> {
    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
    @Override
    public String visit(MainClass n, SymbolTable argu) throws Exception {
        String classname = n.f1.accept(this, null);

        // Add main class to symbol table with null parent.
        ClassSymbolTable newClass = new ClassSymbolTable(classname, null);
        argu.table.put(classname, newClass);
        argu.enter(newClass);
        
        MethodSymbolTable newMethod = new MethodSymbolTable("void");
        newClass.methods.put("main", newMethod);
        newClass.enter(newMethod);

        String varName = n.f11.accept(this, argu);
        newMethod.params.put(varName, "string[]");

        // Check method variables.
        n.f14.accept(this, argu);

        // Check method statements.
        n.f15.accept(this, argu);

        newClass.exit();

        argu.exit();

        return null;
    }
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, SymbolTable argu) throws Exception {
        String classname = n.f1.accept(this, null);
        
        // Check if class has already been declared.
        if(argu.table.containsKey(classname)){
            throw new Exception("Duplicate class name.");
        }
        // Else, add class to symbol table with null parent.
        ClassSymbolTable newClass = new ClassSymbolTable(classname, null);
        argu.table.put(classname, newClass);
        argu.enter(newClass);

        n.f3.accept(this, argu);
        n.f4.accept(this, argu);

        argu.exit();

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, SymbolTable argu) throws Exception {
        String classname = n.f1.accept(this, null);
        String parentClass = n.f3.accept(this, null);

        // Check if class has already been declared.
        if(argu.table.containsKey(classname)){
            throw new Exception("Duplicate class name.");
        }

        // Check if parent has been declared.
        if(!argu.table.containsKey(parentClass)){
            throw new Exception("Parent class doesn't exist.");
        }

        // Check parent class does not extend from other class.
        ClassSymbolTable parentC = argu.table.get(parentClass);
        if(parentC.parent != null) {
            throw new Exception("Single inheritance only.");
        }

        // Else, add class to symbol table with null parent.
        ClassSymbolTable newClass = new ClassSymbolTable(classname, parentClass);
        argu.table.put(classname, newClass);
    
        argu.enter(newClass);

        n.f5.accept(this, argu);
        n.f6.accept(this, argu);

        argu.exit();

        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    @Override
    public String visit(VarDeclaration n, SymbolTable argu) throws Exception {
        ClassSymbolTable currentClass = argu.getCurrent();
        MethodSymbolTable currentMethod = currentClass.getCurrent();

        String type = n.f0.accept(this, argu);
        String variable = n.f1.accept(this, argu);
        
        if(currentMethod == null) {
            // Check if variable already exists.
            if(currentClass.fields.containsKey(variable)){
                throw new Exception("Duplicate variable name.");
            }
            // Else, add variable to fields map of current class.
            currentClass.fields.put(variable, type);
        }
        else {
            // Check if variable already exists.
            if(currentMethod.vars.containsKey(variable)){
                throw new Exception("Duplicate variable name.");
            }
            // Else, add variable to fields map of current class.
            currentMethod.vars.put(variable, type);
        }

        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, SymbolTable argu) throws Exception {
        ClassSymbolTable currentClass = argu.getCurrent();

        String myType = n.f1.accept(this, argu);
        String myName = n.f2.accept(this, argu);

        // Check if method already exists in class:
        if(currentClass.methods.containsKey(myName)){
            throw new Exception("Duplicate method name.");
        }

        MethodSymbolTable newMethod = new MethodSymbolTable(myType);
        currentClass.methods.put(myName, newMethod);
        currentClass.enter(newMethod);
        n.f4.accept(this, argu);

        // Check method variables.
        n.f7.accept(this, argu);

        // Check method statements.
        n.f8.accept(this, argu);
        n.f10.accept(this, argu);

        currentClass.exit();

        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, SymbolTable argu) throws Exception{
        ClassSymbolTable currentClass = argu.getCurrent();
        MethodSymbolTable currentMethod = currentClass.getCurrent();

        String type = n.f0.accept(this, argu);
        String name = n.f1.accept(this, argu);

        // Check if parameter already exists.
        if(currentMethod.params.containsKey(name)){
            throw new Exception("Duplicate paramenter name.");
        }
        // Else, add parameter to params map of currect method.
        currentMethod.params.put(name, type);


        return null;
    }

    @Override
    public String visit(ArrayType n, SymbolTable argu) {
        return "int[]";
    }

    public String visit(BooleanType n, SymbolTable argu) {
        return "boolean";
    }

    public String visit(IntegerType n, SymbolTable argu) {
        return "int";
    }

    @Override
    public String visit(Identifier n, SymbolTable argu) {
        return n.f0.toString();
    }
}