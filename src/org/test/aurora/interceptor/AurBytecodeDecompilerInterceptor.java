package org.test.aurora.interceptor;

import org.aurora.binary.AurBinaryCodes;
import org.aurora.binary.AurBytecode;
import org.aurora.compiler.AurCompiledCode;
import org.aurora.compiler.AurInstructionCode;
import org.aurora.interceptor.AurPassiveInterceptor;
import org.aurora.type.AurValueType;

import java.io.*;
import java.util.Base64;

public class AurBytecodeDecompilerInterceptor implements AurPassiveInterceptor<AurCompiledCode, AurBytecode> {

    int offset = 0;
    int depth = 0;
    int tabSize = 4;

    private final PrintWriter writer;

    public AurBytecodeDecompilerInterceptor() {
        try {
            writer = new PrintWriter(new FileWriter("/home/vitor/IdeaProjects/Aurora/project/debug/test.disassemble"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beforeState(AurCompiledCode input) {

    }

    @Override
    public void afterState(AurBytecode input) {
        disassemble(input);
        writer.flush();
        writer.close();
    }

    @Override
    public String getName() {
        return "decompiler";
    }

    private void disassemble(AurBytecode input) {
        for (offset = 0; offset < input.code.length; ) {
            switch (input.code[offset]) {
                case AurBinaryCodes.HEADER:
                    disassembleHeader(input);
                    break;

                case AurBinaryCodes.MAIN:
                    disassembleMain(input);
                    break;

                case AurBinaryCodes.STRING_POOL:
                    disassemblePool(input);
                    break;

                case AurBinaryCodes.TABLE:
                    disassembleTable(input);
                    break;

                case AurBinaryCodes.END_OF_FILE:
                    advance();
                    break;
            }
        }
    }

    private void disassemblePool(AurBytecode input) {
        writeln("STRING_POOL:");
        advance();

        beginScope();

        while (peek(input) != AurBinaryCodes.TABLE) {
            disassembleStringFromPool(input);
        }

        newline();
    }

    private void disassembleStringFromPool(AurBytecode input) {
        byte length = peek(input);
        advance();

        byte[] bytes = new byte[length];

        for (int i = 0; i < length; i++) {
            bytes[i] = peek(input);
            advance();
        }

        String decodedString = new String(bytes);
        writelnIndented(decodedString);
    }

    private void disassembleMain(AurBytecode input) {
        writeln("MAIN:");
        advance();

        beginScope();

        while (peek(input) != AurBinaryCodes.STRING_POOL) {
            disassembleInstruction(peek(input), input);
        }

        endScope();

        newline();
        newline();
    }

    private void disassembleTable(AurBytecode input) {
        writeln("TABLE:");
        advance();

        beginScope();

        while (peek(input) != AurBinaryCodes.END_OF_FILE) {
            disassembleTableElement(input);
        }

        endScope();
    }

    private void disassembleTableElement(AurBytecode input) {
        switch (peek(input)) {
            case AurValueType.STRING:
                writeIndented("STRING:");
                advance();
                disassembleString(input);
                break;

            case AurValueType.INT:
                writeIndented("INT:");
                advance();
                disassembleInt(input);
                break;

            case AurValueType.FLOAT:
                writeIndented("FLOAT:");
                advance();
                disassembleFloat(input);
                break;

            case AurValueType.BOOL:
                writeIndented("BOOL:");
                advance();
                disassembleBool(input);
                break;

            case AurValueType.CHAR:
                writeIndented("CHAR:");
                advance();
                disassembleChar(input);
                break;
        }
    }

    private void disassembleChar(AurBytecode input) {
        advance();
        byte charValue = peek(input);
        advance();

        char value = (char) charValue;
        writelnIndented(String.valueOf(value));
    }

    private void disassembleBool(AurBytecode input) {
        byte boolValue = peek(input);
        advance();
        advance();

        boolean value = boolValue != 0;
        writelnIndented(String.valueOf(value));
    }

    private void disassembleFloat(AurBytecode input) {
        int first = Byte.toUnsignedInt(peek(input));
        advance();
        int second = Byte.toUnsignedInt(peek(input));
        advance();
        int third = Byte.toUnsignedInt(peek(input));
        advance();
        int fourth = Byte.toUnsignedInt(peek(input));
        advance();
        advance();

        int intBits = (first << 24) | (second << 16) | (third << 8) | (fourth);
        float floatValue = Float.intBitsToFloat(intBits);

        writelnIndented(sameLineIndent("FLOAT:") + floatValue);
    }

    private void disassembleInt(AurBytecode input) {
        int first = peek(input);
        advance();
        int second = peek(input);
        advance();
        int third = peek(input);
        advance();
        int fourth = peek(input);
        advance();
        advance();

        int intValue = (first << 24) | (second << 16) | (third << 8) | (fourth);

        writelnIndented(sameLineIndent("INT:") + intValue);
    }

    private void disassembleString(AurBytecode input) {
        int length = Byte.toUnsignedInt(peek(input));
        advance();

        byte[] stringBytes = new byte[length];

        for (int i = 0; i < length; i++) {
            stringBytes[i] = peek(input);
            advance();
        }

        advance();

        String decodedString = new String(stringBytes);

        writelnIndented(decodedString);
    }

    private void disassembleInstruction(byte instruction, AurBytecode input) {

        switch (instruction) {
            case AurInstructionCode.DEFINE:
                writeIndented("DEFINE");
                writeIndented("");
                writeIndented("");
                write("   ");
                advance();
                writelnIndented("; " + input.stringTable.get(peek(input)));
                advance();
                break;

            case AurInstructionCode.STORE:
                writeIndented("STORE");
                writeIndented("");
                writeIndented("");
                write("    ");
                advance();
                writelnIndented("; " + input.stringTable.get(peek(input)));
                advance();
                break;

            case AurInstructionCode.LOAD:
                writeIndented("LOAD ");
                writeIndented(" ");
                advance();
                byte index = peek(input);
                advance();
                writeIndented("(" + index + ")");
                writelnIndented("; " + input.stringTable.get(index));
                break;

            case AurInstructionCode.LOAD_CONST:
                writeIndented("LOAD_CONST");
                advance();
                writeIndented("(" + peek(input) + ")");
                writelnIndented("; " + input.constantTable.get(peek(input)));
                advance();
                break;

            case AurInstructionCode.RETURN:
                writelnIndented("RETURN");
                advance();
                break;

            case AurInstructionCode.ADD:
                writelnIndented("ADD");
                advance();
                break;

            case AurInstructionCode.SUB:
                writelnIndented("SUB");
                advance();
                break;

            case AurInstructionCode.MUL:
                writelnIndented("MUL");
                advance();
                break;

            case AurInstructionCode.DIV:
                writelnIndented("DIV");
                advance();
                break;

            case AurInstructionCode.LESS:
                writelnIndented("LESS");
                advance();
                break;

            case AurInstructionCode.LESS_EQUAL:
                writelnIndented("LESS_EQUAL");
                advance();
                break;

            case AurInstructionCode.GREATER:
                writelnIndented("GREATER");
                advance();
                break;

            case AurInstructionCode.GREATER_EQUAL:
                writelnIndented("GREATER_EQUAL");
                advance();
                break;

            case AurInstructionCode.EQUAL_EQUAL:
                writelnIndented("EQUAL_EQUAL");
                advance();
                break;

            case AurInstructionCode.MARK_EQUAL:
                writelnIndented("NOT_EQUAL");
                advance();
                break;

            case AurInstructionCode.NEGATE:
                writelnIndented("NEGATE");
                advance();
                break;

            case AurInstructionCode.INVERSE:
                writelnIndented("INVERSE");
                advance();
                break;

            case AurInstructionCode.AND:
                writelnIndented("AND");
                advance();
                break;

            case AurInstructionCode.OR:
                writelnIndented("OR");
                advance();
                break;

            case AurInstructionCode.PRINT:
                writelnIndented("PRINT");
                advance();
                break;

            case AurInstructionCode.JUMP: {
                writeIndented("BRANCH");
                writeIndented("");

                advance();
                byte lowByte = peek(input);
                advance();
                byte highByte = peek(input);

                short offset = (short) ((highByte << 8) | (lowByte & 0xFF));
                advance();

                writelnIndented("(" + offset + ")");
                break;
            }

            case AurInstructionCode.LOOP: {
                writeIndented("LOOP ");
                writeIndented(" ");

                advance();
                byte lowByte = peek(input);
                advance();
                byte highByte = peek(input);

                short offset = (short) ((highByte << 8) | (lowByte & 0xFF));
                advance();

                writelnIndented("(" + offset + ")");
                break;
            }

            case AurInstructionCode.JUMP_IF_FALSE: {
                writeIndented("B_IF_F");
                writeIndented("");
                advance();
                byte lowByte = peek(input);
                advance();
                byte highByte = peek(input);

                short offset = (short) ((highByte << 8) | (lowByte & 0xFF));
                advance();

                writelnIndented("(" + offset + ")");
                break;
            }
        }
    }

    private void disassembleHeader(AurBytecode input) {
        advance();
        writelnIndented("HEADER:");

        byte[] hashBytes = new byte[80];
        for (int i = 0; i < hashBytes.length; i++) {
            hashBytes[i] = input.code[offset];
            advance();
        }

        String hashString = Base64.getEncoder().encodeToString(hashBytes);

        beginScope();
        writelnIndented(hashString);
        endScope();
        newline();
    }

    private byte peek(AurBytecode input) {
        return input.code[offset];
    }

    private void advance() {
        offset++;
    }

    private void beginScope() {
        depth++;
    }

    private void endScope() {
        depth--;
    }

    private String indent() {
        return " ".repeat(depth * tabSize);
    }

    private void write(String text) {
        writer.print(text);
    }

    private void writeln(String text) {
        writer.println(text);
    }

    private void writeIndented(String text) {
        writer.print(indent() + text);
    }

    private void writelnIndented(String text) {
        writer.println(indent() + text);
    }

    private void writelnIndentedSameLine(String text) {
        writer.println(sameLineIndent(text) + text);
    }

    private void newline() {
        writeln("");
    }

    private String sameLineIndent(String value) {
        return " ".repeat((depth + 1) * tabSize - value.length());
    }
}
