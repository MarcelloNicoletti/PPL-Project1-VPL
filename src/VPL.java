import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class VPL {
    static final int maxIndex = 10000;
    // op to produce comment on a line by itself
    private static final int noOpCode = 0;
    // ops involved with registers
    private static final int labelCode = 1;
    private static final int callCode = 2;
    private static final int passCode = 3;

    // use symbolic names for all opcodes:
    private static final int allocCode = 4;
    private static final int returnCode = 5;  // return a means "return and put
    // copy of value stored in cell a in register returnValue
    private static final int getRetvalCode = 6;//op a means "copy returnValue into cell a"
    private static final int jumpCode = 7;
    private static final int condJumpCode = 8;
    // arithmetic ops
    private static final int addCode = 9;
    private static final int subCode = 10;
    private static final int multCode = 11;
    private static final int divCode = 12;
    private static final int remCode = 13;
    private static final int equalCode = 14;
    private static final int notEqualCode = 15;
    private static final int lessCode = 16;
    private static final int lessEqualCode = 17;
    private static final int andCode = 18;
    private static final int orCode = 19;
    private static final int notCode = 20;
    private static final int oppCode = 21;
    // ops involving transfer of data
    private static final int litCode = 22;  // litCode a b means "cell a gets b"
    private static final int copyCode = 23;// copy a b means "cell a gets cell b"
    private static final int getCode = 24; // op a b means "cell a gets
    // contents of cell whose
    // index is stored in b"
    private static final int putCode = 25;  // op a b means "put contents
    // system-level ops:
    private static final int haltCode = 26;
    private static final int inputCode = 27;
    private static final int outputCode = 28;
    private static final int newlineCode = 29;
    // of cell b in cell whose offset is stored in cell a"
    private static final int symbolCode = 30;
    private static final int newCode = 31;
    // global variable ops:
    private static final int allocGlobalCode = 32;
    private static final int toGlobalCode = 33;
    private static final int fromGlobalCode = 34;
    // debug ops:
    private static final int debugCode = 42;

    // Return info spacer:
    private static final int reservedForReturn = 2;

    static int[] mem = new int[maxIndex];
    static int instructionPointer, basePointer, stackPointer, returnValue, heapPointer, numPassed, globalVarsStart;
    static String fileName;

    public static void main (String[] args) throws Exception {
        BufferedReader keys = new BufferedReader(
                new InputStreamReader(System.in));
        System.out.print("enter name of file containing VPL program: ");
        fileName = keys.readLine();

        // load the program into the front part of
        // memory
        BufferedReader input = new BufferedReader(new FileReader(fileName));
        String line;
        StringTokenizer st;
        int currentOpCode;

        ArrayList<IntPair> labels, holes;
        labels = new ArrayList<>();
        holes = new ArrayList<>();
        int label;

        int firstEmptyCell = 0;
        do {
            line = input.readLine();
            System.out.println("parsing line [" + line + "]");
            if (line != null) {// extract any tokens
                st = new StringTokenizer(line);
                if (st.countTokens() > 0) {// have a token, so must be an instruction (as opposed to empty line)

                    currentOpCode = Integer.parseInt(st.nextToken());

                    // load the instruction into memory:

                    if (currentOpCode == labelCode) {// note index that comes where label would go
                        label = Integer.parseInt(st.nextToken());
                        labels.add(new IntPair(label, firstEmptyCell));
                    } else {// opcode actually gets stored
                        mem[firstEmptyCell] = currentOpCode;
                        firstEmptyCell++;

                        if (currentOpCode == callCode || currentOpCode == jumpCode ||
                                currentOpCode == condJumpCode) {// note the hole immediately after the opcode to be filled in later
                            label = Integer.parseInt(st.nextToken());
                            mem[firstEmptyCell] = label;
                            holes.add(new IntPair(firstEmptyCell, label));
                            firstEmptyCell++;
                        }

                        // load correct number of arguments (following label, if any):
                        for (int j = 0; j < numArgs(currentOpCode); j++) {
                            mem[firstEmptyCell] = Integer.parseInt(st.nextToken());
                            firstEmptyCell++;
                        }

                    }// not a label

                }// have a token, so must be an instruction
            }// have a line
        } while (line != null);

        //System.out.println("after first scan:");
        //showMem( 0, firstEmptyCell-1 );

        // fill in all the holes:
        int index;
        for (int m = 0; m < holes.size(); m++) {
            label = holes.get(m).second;
            index = -1;
            for (int n = 0; n < labels.size(); n++) {
                if (labels.get(n).first == label) {
                    index = labels.get(n).second;
                }
            }
            mem[holes.get(m).first] = index;
        }

        System.out.println("after replacing labels:");
        showMem(0, firstEmptyCell - 1);

        // initialize registers:
        basePointer = firstEmptyCell;
        stackPointer = basePointer + 2;
        instructionPointer = 0;
        returnValue = -1;
        heapPointer = maxIndex;
        numPassed = 0;

        int endOfCodeBlock = basePointer - 1;

        System.out.println("Code is ");
        showMem(0, endOfCodeBlock);

        globalVarsStart = endOfCodeBlock + 1;
        int numGlobalVars = 0;

        boolean doHalt = false;
        boolean doDebug = false;

        do {
            // get Operation code
            int opCode = mem[instructionPointer];
            instructionPointer++;

            // get arguments
            int arg0 = 0, arg1 = 0, arg2 = 0;
            int numArgs = numArgs(opCode);
            if (numArgs == 1) {
                arg0 = mem[instructionPointer];
                instructionPointer++;
            } else if (numArgs == 2) {
                arg0 = mem[instructionPointer];
                instructionPointer++;
                arg1 = mem[instructionPointer];
                instructionPointer++;
            } else if (numArgs == 3) {
                arg0 = mem[instructionPointer];
                instructionPointer++;
                arg1 = mem[instructionPointer];
                instructionPointer++;
                arg2 = mem[instructionPointer];
                instructionPointer++;
            }

            // get start of local vars on stack frame.
            int localVarsStart = basePointer + reservedForReturn;
            // do debugging
            if (doDebug) {
                System.out.print("op:" + opCode);
                System.out.print("arg0:" + arg0);
                System.out.print("arg1:" + arg1);
                System.out.print("arg2:" + arg2);
                System.out.println();
                if (numGlobalVars != 0) {
                    System.out.print("global vars:");
                    showMem(globalVarsStart, globalVarsStart + numGlobalVars);
                }
                System.out.print("local vars:");
                showMem(basePointer, stackPointer + 2 + numPassed);
                System.in.read();
            }

            // do operations
            if (opCode == callCode) { // 2 (0 is no-op, 1 is preprocessed out)
                // call L
                mem[stackPointer] = basePointer;
                mem[stackPointer + 1] = instructionPointer;
                instructionPointer = arg0;
                basePointer = stackPointer;
                stackPointer += numPassed + 2;
                numPassed = 0;
            } else if (opCode == passCode) {
                // pass a
                mem[stackPointer + reservedForReturn + numPassed] = mem[localVarsStart + arg0];
                numPassed++;
            } else if (opCode == allocCode) {
                // alloc n
                stackPointer += arg0;
            } else if (opCode == returnCode) { // 5
                // return a
                instructionPointer = mem[basePointer + 1];
                stackPointer = basePointer;
                basePointer = mem[basePointer];
                returnValue = mem[localVarsStart + arg0];
            } else if (opCode == getRetvalCode) {
                // getRetVal a
                mem[localVarsStart + arg0] = returnValue;
            } else if (opCode == jumpCode) {
                // jump L
            } else if (opCode == condJumpCode) {
                // condJump L a
            } else if (opCode == addCode) {
                // add a b c
                mem[localVarsStart + arg0] = mem[localVarsStart + arg1] + mem[localVarsStart + arg2];
            } else if (opCode == subCode) { // 10
                // sub a b c
                mem[localVarsStart + arg0] = mem[localVarsStart + arg1] - mem[localVarsStart + arg2];
            } else if (opCode == multCode) {
                // mult a b c
                mem[localVarsStart + arg0] = mem[localVarsStart + arg1] * mem[localVarsStart + arg2];
            } else if (opCode == divCode) {
                // div a b c
                mem[localVarsStart + arg0] = mem[localVarsStart + arg1] / mem[localVarsStart + arg2];
            } else if (opCode == remCode) {
                // rem a b c
                mem[localVarsStart + arg0] = mem[localVarsStart + arg1] % mem[localVarsStart + arg2];
            } else if (opCode == equalCode) {
                // eq a b c
                mem[localVarsStart + arg0] = (mem[localVarsStart + arg1] == mem[localVarsStart + arg2]) ? 1 : 0;
            } else if (opCode == notEqualCode) { // 15
                // neq a b c
                mem[localVarsStart + arg0] = (mem[localVarsStart + arg1] != mem[localVarsStart + arg2]) ? 1 : 0;
            } else if (opCode == lessCode) {
                // lt a b c
                mem[localVarsStart + arg0] = (mem[localVarsStart + arg1] < mem[localVarsStart + arg2]) ? 1 : 0;
            } else if (opCode == lessEqualCode) {
                // lte a b c
                mem[localVarsStart + arg0] = (mem[localVarsStart + arg1] <= mem[localVarsStart + arg2]) ? 1 : 0;
            } else if (opCode == andCode) {
                // and a b c
                mem[localVarsStart + arg0] = ((mem[localVarsStart + arg1] != 0) && (mem[localVarsStart + arg2] != 0)) ? 1 : 0;
            } else if (opCode == orCode) {
                // or a b c
                mem[localVarsStart + arg0] = ((mem[localVarsStart + arg1] != 0) || (mem[localVarsStart + arg2] != 0)) ? 1 : 0;
            } else if (opCode == notCode) { // 20
                // not a b
                mem[localVarsStart + arg0] = (mem[localVarsStart + arg1] == 0) ? 1 : 0;
            } else if (opCode == oppCode) {
                // opp a b
                // TODO: Is this correct?
                mem[localVarsStart + arg0] = -mem[localVarsStart + arg1];
            } else if (opCode == litCode) {
                // lit a n
                mem[localVarsStart + arg0] = arg1;
            } else if (opCode == copyCode) {
                // cp a b
                mem[localVarsStart + arg0] = mem[localVarsStart + arg1];
            } else if (opCode == getCode) {
                // get a b c
            } else if (opCode == putCode) { // 25
                // put a b c
            } else if (opCode == haltCode) {
                doHalt = true;
            } else if (opCode == inputCode) {
                // in a
            } else if (opCode == outputCode) {
                // out a
            } else if (opCode == newlineCode) {
                // nl
            } else if (opCode == symbolCode) { // 30
                // sym a
            } else if (opCode == newCode) {
                // new a b
            } else if (opCode == allocGlobalCode) {
                // galloc n
            } else if (opCode == toGlobalCode) {
                // cp2g n a
            } else if (opCode == fromGlobalCode) { // 34 - end of spec
                // cpFg a n
            } else if (opCode == debugCode) { // 42
                // debug (not in lang spec)
                doDebug = !doDebug;
            } else {
                throwException("Unknown opcode [" + opCode + "]");
            }

            }
            opCode = arg0 = arg1 = arg2 = 0; // Reset operation specific
        } while (!doHalt);

    }// main

    // return the number of arguments after the opcode,
    // except ops that have a label return number of arguments
    // after the label, which always comes immediately after
    // the opcode
    private static int numArgs (int opCode) {
        // highlight specially behaving operations
        if (opCode == labelCode) {
            return 1;  // not used
        } else if (opCode == jumpCode) {
            return 0;  // jump label
        } else if (opCode == condJumpCode) {
            return 1;  // condJump label expr
        } else if (opCode == callCode) {
            return 0;  // call label
        } else if (opCode == debugCode) {
            return 0; // debug
        }

        // for all other ops, lump by count:

        else if (opCode == noOpCode ||
                opCode == haltCode ||
                opCode == newlineCode ||
                opCode == debugCode
                ) {
            return 0;  // op
        } else if (opCode == passCode || opCode == allocCode ||
                opCode == returnCode || opCode == getRetvalCode ||
                opCode == inputCode ||
                opCode == outputCode || opCode == symbolCode ||
                opCode == allocGlobalCode
                ) {
            return 1;  // op arg1
        } else if (opCode == notCode || opCode == oppCode ||
                opCode == litCode || opCode == copyCode || opCode == newCode ||
                opCode == toGlobalCode || opCode == fromGlobalCode

                ) {
            return 2;  // op arg1 arg2
        } else if (opCode == addCode || opCode == subCode || opCode == multCode ||
                opCode == divCode || opCode == remCode || opCode == equalCode ||
                opCode == notEqualCode || opCode == lessCode ||
                opCode == lessEqualCode || opCode == andCode ||
                opCode == orCode || opCode == getCode || opCode == putCode
                ) {
            return 3;
        } else {
            throwException("Unknown opcode [" + opCode + "]");
            return -1;
        }

    }// numArgs

    private static void showMem (int startIndex, int stopIndex) {
        for (int currentIndex = startIndex; currentIndex <= stopIndex; currentIndex++) {
            System.out.println(currentIndex + ": " + mem[currentIndex]);
        }
    }// showMem

    private static void throwException(String message) {
        System.out.println("Fatal Error:" + message);
        System.exit(1);
    }
}// VPL
