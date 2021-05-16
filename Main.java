import syntaxtree.*;
import visitor.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length != 1){
            System.err.println("Usage: java Main <inputFile>");
            System.exit(1);
        }


        FileInputStream fis = null;
        try{
            fis = new FileInputStream(args[0]);
            MiniJavaParser parser = new MiniJavaParser(fis);

            Goal root = parser.Goal();

            System.err.println("Program parsed successfully.");
            
            SymbolTable table = new SymbolTable();
            DeclVisitor eval = new DeclVisitor();
            root.accept(eval, table);

            table.printTableInfo();

            typeCheckVisitor eval2 = new typeCheckVisitor();
            root.accept(eval2, table);

        }
        catch(ParseException ex){
            System.out.println(ex.getMessage());
        }
        catch(FileNotFoundException ex){
            System.err.println(ex.getMessage());
        }
        finally{
            try{
                if(fis != null) fis.close();
            }
            catch(IOException ex){
                System.err.println(ex.getMessage());
            }
        }
    }
}


class SymbolTable {
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

class DeclVisitor extends GJDepthFirst<String, SymbolTable> {
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
        classSymbolTable newClass = new classSymbolTable(classname, null);
        argu.table.put(classname, newClass);
        argu.enter(newClass);
        
        methodSymbolTable newMethod = new methodSymbolTable("void");
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
        classSymbolTable newClass = new classSymbolTable(classname, null);
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
        classSymbolTable parentC = argu.table.get(parentClass);
        if(parentC.parent != null) {
            throw new Exception("Single inheritance only.");
        }

        // Else, add class to symbol table with null parent.
        classSymbolTable newClass = new classSymbolTable(classname, parentClass);
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
        classSymbolTable currentClass = argu.getCurrent();
        methodSymbolTable currentMethod = currentClass.getCurrent();

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
        classSymbolTable currentClass = argu.getCurrent();

        String myType = n.f1.accept(this, argu);
        String myName = n.f2.accept(this, argu);

        // Check if method already exists in class:
        if(currentClass.methods.containsKey(myName)){
            throw new Exception("Duplicate method name.");
        }

        methodSymbolTable newMethod = new methodSymbolTable(myType);
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
        classSymbolTable currentClass = argu.getCurrent();
        methodSymbolTable currentMethod = currentClass.getCurrent();

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

class typeCheckVisitor extends GJDepthFirst<String, SymbolTable>{
    @Override
    public String visit(ClassDeclaration n, SymbolTable argu) throws Exception {
        String classname = n.f1.accept(this, argu);
        classSymbolTable current = argu.table.get(classname);
        
        argu.enter(current);

        n.f3.accept(this, argu);
        n.f4.accept(this, argu);

        argu.exit();

        return null;
    }

    @Override
    public String visit(ClassExtendsDeclaration n, SymbolTable argu) throws Exception {
        String classname = n.f1.accept(this, argu);
        classSymbolTable current = argu.table.get(classname);
    
        argu.enter(current);

        n.f5.accept(this, argu);
        n.f6.accept(this, argu);

        argu.exit();

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
        classSymbolTable currentClass = argu.getCurrent();

        String myName = n.f2.accept(this, argu);

        System.out.println(myName);
        methodSymbolTable current = currentClass.methods.get(myName);

        currentClass.enter(current);

        // Check method variables.
        n.f7.accept(this, argu);

        // Check method statements.
        n.f8.accept(this, argu);

        currentClass.exit();

        return null;
    }

    @Override
    public String visit(ArrayType n, SymbolTable argu) {
        return "int[]";
    }

    @Override
    public String visit(BooleanType n, SymbolTable argu) {
        return "boolean";
    }

    @Override
    public String visit(IntegerType n, SymbolTable argu) {
        return "int";
    }

    @Override
    public String visit(Identifier n, SymbolTable argu) throws Exception {
        String name = n.f0.toString();

        classSymbolTable currentClass = argu.getCurrent();
        methodSymbolTable currentMethod = null;
        if(currentClass != null) {
            currentMethod = currentClass.getCurrent();
        }

        // Not in method.
        if(currentMethod == null && currentClass != null){
            // Search for class names.
            if(argu.table.containsKey(name)) {
                return name;
            }
            // Search for field in current class.
            if(currentClass.fields.containsKey(name)) {
                return currentClass.fields.get(name);
            }
            // Search for method in current class.
            if(currentClass.methods.containsKey(name)) {
                return name;
            }           
            classSymbolTable temp = currentClass;
            while(temp.parent != null) {
                temp = argu.table.get(temp.parent);
                // Search for field in parent class.
                if(temp.fields.containsKey(name)) {
                    return temp.fields.get(name);
                }
                // Search for method in parent class.
                if(temp.methods.containsKey(name)) {
                    return name;
                }           
            } 
        }
        // In method.
        else if(currentClass != null) {
            // Search for parameter in current method.
            if(currentMethod.params.containsKey(name)) {
                return currentMethod.params.get(name);
            }
            // Search for variable in current method.
            if(currentMethod.vars.containsKey(name)) {
                return currentMethod.vars.get(name);
            }
            
            // Search for class names.
            if(argu.table.containsKey(name)) {
                return name;
            }
            // Search for field in current class.
            if(currentClass.fields.containsKey(name)) {
                return currentClass.fields.get(name);
            }
            // Search for method in current class.
            if(currentClass.methods.containsKey(name)) {
                return name;
            }            

            classSymbolTable temp = currentClass;
            while(temp.parent != null) {
                temp = argu.table.get(temp.parent);
                // Search for field in parent class.
                if(temp.fields.containsKey(name)) {
                    return temp.fields.get(name);
                }
                // Search for method in parent class.
                if(temp.methods.containsKey(name)) {
                    return name;
                }           
            } 
        }

        if(currentClass == null) {
            return name;
        }

        throw new Exception("Identifier " + name + " not found.");
    }

    @Override
    public String visit(ThisExpression n, SymbolTable argu) throws Exception {
        return "this";
    }

    /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | NotExpression()
    *       | BracketExpression()
    */
    @Override
    public String visit(PrimaryExpression n, SymbolTable argu) throws Exception {
        String ret = n.f0.accept(this, argu);

        if(ret.equals("this")) {
            classSymbolTable current = argu.getCurrent();
            return current.name;
        }

        return ret;
    }

    @Override
    public String visit(IntegerLiteral n, SymbolTable argu) throws Exception {
        return "int";
    }

    @Override
    public String visit(TrueLiteral n, SymbolTable argu) throws Exception {
        return "boolean";
     }
  
    @Override
    public String visit(FalseLiteral n, SymbolTable argu) throws Exception {
        return "boolean";
    }

    /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    @Override
    public String visit(ArrayAllocationExpression n, SymbolTable argu) throws Exception {
        // Check if expression is integer.
        String exp = n.f3.accept(this, argu);
        if(!exp.equals("int")) {
            throw new Exception("Array size should be integer.");
        }
        return "int[]";
    }

    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    @Override
    public String visit(BracketExpression n, SymbolTable argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public String visit(AssignmentStatement n, SymbolTable argu) throws Exception {
        String identifier = n.f0.accept(this, argu);
        String expr = n.f2.accept(this, argu);
        if(!identifier.equals(expr)) {
            System.out.println(identifier + " != " + expr);
            throw new Exception("Type mismatch in assignment.");
        }
        return identifier;
    }

    /**
    * f0 -> "!"
    * f1 -> PrimaryExpression()
    */
    public String visit(NotExpression n, SymbolTable argu) throws Exception {
        String expr = n.f1.accept(this, argu);
        if(!expr.equals("boolean")) {
            throw new Exception("Expression in \"not\" should be boolean.");
        }
        return "boolean";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "&&"
    * f2 -> PrimaryExpression()
    */
    public String visit(AndExpression n, SymbolTable argu) throws Exception {
        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);

        if(!left.equals("boolean")) {
            throw new Exception("Expressions in \"&&\" should be boolean." + left + " " + right);
        }
        if(!right.equals("boolean")) {
            throw new Exception("Expressions in \"&&\" should be boolean." + left + " " + right);
        }

        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    public String visit(CompareExpression n, SymbolTable argu) throws Exception {
        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);

        if(!left.equals("int")) {
            throw new Exception("Expressions in \"<\" should be boolean." + left + " " + right);
        }
        if(!right.equals("int")) {
            throw new Exception("Expressions in \"<\" should be boolean." + left + " " + right);
        }

        return "boolean";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public String visit(PlusExpression n, SymbolTable argu) throws Exception {
        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);
        
        if(!left.equals("int")) {
            throw new Exception("Expressions in \"+\" should be integers.");
        }
        if(!right.equals("int")) {
            throw new Exception("Expressions in \"+\" should be integers.");
        }

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public String visit(MinusExpression n, SymbolTable argu) throws Exception {
        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);
        
        if(!left.equals("int")) {
            throw new Exception("Expressions in \"-\" should be integers.");
        }
        if(!right.equals("int")) {
            throw new Exception("Expressions in \"-\" should be integers.");
        }

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    public String visit(TimesExpression n, SymbolTable argu) throws Exception {
        String left = n.f0.accept(this, argu);
        String right = n.f2.accept(this, argu);
        
        if(!left.equals("int")) {
            throw new Exception("Expressions in \"*\" should be integers.");
        }
        if(!right.equals("int")) {
            throw new Exception("Expressions in \"*\" should be integers.");
        }

        return "int";
    }

     /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String visit(ArrayLookup n, SymbolTable argu) throws Exception {
        String name = n.f0.accept(this, argu);
        String position = n.f2.accept(this, argu);

        if(!name.equals("int[]")) {
            throw new Exception("Not array.");
        }
        if(!position.equals("int")) {
            throw new Exception("Array position must be integer.");
        }
        
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public String visit(ArrayLength n, SymbolTable argu) throws Exception {
        String name = n.f0.accept(this, argu);

        if(!name.equals("int[]")) {
            throw new Exception("Not array.");
        }

        return "int";
    }

    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String visit(AllocationExpression n, SymbolTable argu) throws Exception {
        String name = n.f1.accept(this, argu);
        return name;
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    public String visit(MessageSend n, SymbolTable argu) throws Exception {
        String expr = n.f0.accept(this, argu);

        classSymbolTable currentClass = argu.getCurrent();

        classSymbolTable temp = null;
        if(!argu.table.containsKey(expr)) {
            throw new Exception("Can't find class.");
        }
        temp = argu.table.get(expr); 

        argu.enter(temp);
        String ident = n.f2.accept(this, argu);
        String identType = temp.methods.get(ident).type;

        argu.params = new ArrayList<>();
        for(Map.Entry<String, String> entry : temp.methods.get(ident).params.entrySet()) {
            argu.params.add(entry.getValue());
        }
        argu.currentIndex = 0;
        
        argu.enter(currentClass);
        
        n.f4.accept(this, argu);
        if(argu.currentIndex < argu.params.size()) {
            throw new Exception("Wrong number of parameters.");
        }

        return identType;
    }

    /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    @Override
    public String visit(ArrayAssignmentStatement n, SymbolTable argu) throws Exception {
        String ident = n.f0.accept(this, argu);
        String left = n.f2.accept(this, argu);
        String right = n.f5.accept(this, argu);

        if(!ident.equals("int[]")) {
            throw new Exception("Array must be array.");
        }

        if(!left.equals("int")) {
            throw new Exception("Array position must be int.");
        }

        if(!right.equals("int")) {
            throw new Exception("Array value must be int.");
        }

        return null;
    }

    /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    public String visit(IfStatement n, SymbolTable argu) throws Exception {
        String expr = n.f2.accept(this, argu);

        if(!expr.equals("boolean")) {
            throw new Exception("If expression must be boolean.");
        }

        n.f4.accept(this, argu);
        n.f6.accept(this, argu);
        return null;
    }

    /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    public String visit(WhileStatement n, SymbolTable argu) throws Exception {
        String expr = n.f2.accept(this, argu);

        if(!expr.equals("boolean")) {
            throw new Exception("While expression must be boolean.");
        }

        n.f4.accept(this, argu);
        return null;
    }

    /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public String visit(ExpressionList n, SymbolTable argu) throws Exception {
        if(argu.params.size() == 0) {
            throw new Exception("Wrong number of parameters.");
        }

        String param = argu.params.get(0);
        String expr = n.f0.accept(this, argu);

        if(!param.equals(expr)) {
            throw new Exception("Wrong parameter type. " + param + "!=" + expr);
        }

        argu.currentIndex = 1;
        n.f1.accept(this, argu);
        return null;
     }

    /**
    * f0 -> ","
    * f1 -> Expression()
    */
    public String visit(ExpressionTerm n, SymbolTable argu) throws Exception {
        if(argu.currentIndex > argu.params.size() - 1) {
            throw new Exception("Wrong number of parameters.");
        }
        
        String param = argu.params.get(argu.currentIndex);
        String expr = n.f1.accept(this, argu);

        if(!param.equals(expr)) {
            throw new Exception("Wrong parameter type. " + param + "!=" + expr);
        }

        argu.currentIndex = argu.currentIndex + 1;
        return null;
    }
}