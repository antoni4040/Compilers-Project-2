import syntaxtree.*;
import visitor.*;

import java.util.Map;
import java.util.ArrayList;

public class TypeCheckVisitor extends GJDepthFirst<String, SymbolTable>{
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
        String classname = n.f1.accept(this, argu);

        // Add main class to symbol table with null parent.
        ClassSymbolTable newClass = argu.table.get(classname);
        argu.enter(newClass);
        
        MethodSymbolTable newMethod = newClass.methods.get("main");
        newClass.enter(newMethod);

        // Check method variables.
        n.f14.accept(this, argu);

        // Check method statements.
        n.f15.accept(this, argu);

        newClass.exit();

        argu.exit();

        return null;
    }
    
    @Override
    public String visit(ClassDeclaration n, SymbolTable argu) throws Exception {
        String classname = n.f1.accept(this, argu);
        ClassSymbolTable current = argu.table.get(classname);
        
        argu.enter(current);

        n.f3.accept(this, argu);
        n.f4.accept(this, argu);

        argu.exit();

        return null;
    }

    @Override
    public String visit(ClassExtendsDeclaration n, SymbolTable argu) throws Exception {
        String classname = n.f1.accept(this, argu);
        ClassSymbolTable current = argu.table.get(classname);
    
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
        ClassSymbolTable currentClass = argu.getCurrent();

        String myName = n.f2.accept(this, argu);

        System.out.println(myName);
        MethodSymbolTable current = currentClass.methods.get(myName);

        currentClass.enter(current);

        // Check method variables.
        n.f7.accept(this, argu);

        // Check method statements.
        n.f8.accept(this, argu);

        String ret = n.f10.accept(this, argu);
        if(!current.type.equals(ret)) {
            
            boolean matchFound = false;
            if(argu.table.containsKey(ret)) {
                ClassSymbolTable currentC = argu.table.get(ret);
                while(currentC.parent != null) {
                    if(currentC.parent == current.type) {
                        matchFound = true;
                        break;
                    }
                    currentC = argu.table.get(currentC.parent);
                }
            }

            if(!matchFound) {
                System.out.println(current.type + " != " + ret);
                throw new Exception("Type mismatch in return type.");
            }
        }

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

        ClassSymbolTable currentClass = argu.getCurrent();
        MethodSymbolTable currentMethod = null;
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
            ClassSymbolTable temp = currentClass;
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

            ClassSymbolTable temp = currentClass;
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
            ClassSymbolTable current = argu.getCurrent();
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
            
            boolean matchFound = false;
            if(argu.table.containsKey(expr)) {
                ClassSymbolTable current = argu.table.get(expr);
                while(current.parent != null) {
                    if(current.parent == identifier) {
                        matchFound = true;
                        break;
                    }
                    current = argu.table.get(current.parent);
                }
            }

            if(!matchFound) {
                System.out.println(identifier + " != " + expr);
                throw new Exception("Type mismatch in assignment.");
            }
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

        ClassSymbolTable currentClass = argu.getCurrent();

        ClassSymbolTable temp = null;
        if(!argu.table.containsKey(expr)) {
            throw new Exception("Can't find class.");
        }
        temp = argu.table.get(expr); 

        argu.enter(temp);
        String ident = n.f2.accept(this, argu);
        //TODO: search in parents as well.
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
            
            boolean matchFound = false;
            if(argu.table.containsKey(expr)) {
                ClassSymbolTable current = argu.table.get(expr);
                while(current.parent != null) {
                    if(current.parent == param) {
                        matchFound = true;
                        break;
                    }
                    current = argu.table.get(current.parent);
                }
            }

            if(!matchFound) {
                System.out.println(param + " != " + expr);
                throw new Exception("Type mismatch in parameter.");
            }
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
            
            boolean matchFound = false;
            if(argu.table.containsKey(expr)) {
                ClassSymbolTable current = argu.table.get(expr);
                while(current.parent != null) {
                    if(current.parent == param) {
                        matchFound = true;
                        break;
                    }
                    current = argu.table.get(current.parent);
                }
            }

            if(!matchFound) {
                System.out.println(param + " != " + expr);
                throw new Exception("Type mismatch in parameter.");
            }
        }

        argu.currentIndex = argu.currentIndex + 1;
        return null;
    }
}