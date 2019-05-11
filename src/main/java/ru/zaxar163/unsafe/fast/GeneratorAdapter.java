// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package ru.zaxar163.unsafe.fast;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * FROM: ow2 asm-commons
 *
 * A {@link MethodVisitor} with convenient methods to generate code. For
 * example, using this adapter, the class below
 *
 * <pre>
 * public class Example {
 * 	public static void main(String[] args) {
 * 		System.out.println(&quot;Hello world!&quot;);
 * 	}
 * }
 * </pre>
 *
 * <p>
 * can be generated as follows:
 *
 * <pre>
 * ClassWriter cw = new ClassWriter(0);
 * cw.visit(V1_1, ACC_PUBLIC, &quot;Example&quot;, null, &quot;java/lang/Object&quot;, null);
 *
 * Method m = Method.getMethod(&quot;void &lt;init&gt; ()&quot;);
 * GeneratorAdapter mg = new GeneratorAdapter(ACC_PUBLIC, m, null, null, cw);
 * mg.loadThis();
 * mg.invokeConstructor(Type.getType(Object.class), m);
 * mg.returnValue();
 * mg.endMethod();
 *
 * m = Method.getMethod(&quot;void main (String[])&quot;);
 * mg = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m, null, null, cw);
 * mg.getStatic(Type.getType(System.class), &quot;out&quot;, Type.getType(PrintStream.class));
 * mg.push(&quot;Hello world!&quot;);
 * mg.invokeVirtual(Type.getType(PrintStream.class), Method.getMethod(&quot;void println (String)&quot;));
 * mg.returnValue();
 * mg.endMethod();
 *
 * cw.visitEnd();
 * </pre>
 *
 * @author Juozas Baliuka
 * @author Chris Nokleberg
 * @author Eric Bruneton
 * @author Prashant Deva
 */
class GeneratorAdapter extends LocalVariablesSorter {

	/** Constant for the {@link #math} method. */
	public static final int ADD = Opcodes.IADD;

	/** Constant for the {@link #math} method. */
	public static final int AND = Opcodes.IAND;

	private static final Type BOOLEAN_TYPE = Type.getObjectType("java/lang/Boolean");

	private static final Method BOOLEAN_VALUE = Method.getMethod("boolean booleanValue()");

	private static final Type BYTE_TYPE = Type.getObjectType("java/lang/Byte");

	private static final Method CHAR_VALUE = Method.getMethod("char charValue()");

	private static final Type CHARACTER_TYPE = Type.getObjectType("java/lang/Character");

	private static final String CLASS_DESCRIPTOR = "Ljava/lang/Class;";

	/** Constant for the {@link #math} method. */
	public static final int DIV = Opcodes.IDIV;

	private static final Type DOUBLE_TYPE = Type.getObjectType("java/lang/Double");

	private static final Method DOUBLE_VALUE = Method.getMethod("double doubleValue()");

	/** Constant for the {@link #ifCmp} method. */
	public static final int EQ = Opcodes.IFEQ;

	private static final Type FLOAT_TYPE = Type.getObjectType("java/lang/Float");

	private static final Method FLOAT_VALUE = Method.getMethod("float floatValue()");

	/** Constant for the {@link #ifCmp} method. */
	public static final int GE = Opcodes.IFGE;

	/** Constant for the {@link #ifCmp} method. */
	public static final int GT = Opcodes.IFGT;

	private static final Method INT_VALUE = Method.getMethod("int intValue()");

	private static final Type INTEGER_TYPE = Type.getObjectType("java/lang/Integer");

	/** Constant for the {@link #ifCmp} method. */
	public static final int LE = Opcodes.IFLE;

	private static final Type LONG_TYPE = Type.getObjectType("java/lang/Long");

	private static final Method LONG_VALUE = Method.getMethod("long longValue()");

	/** Constant for the {@link #ifCmp} method. */
	public static final int LT = Opcodes.IFLT;

	/** Constant for the {@link #math} method. */
	public static final int MUL = Opcodes.IMUL;

	/** Constant for the {@link #ifCmp} method. */
	public static final int NE = Opcodes.IFNE;

	/** Constant for the {@link #math} method. */
	public static final int NEG = Opcodes.INEG;

	private static final Type NUMBER_TYPE = Type.getObjectType("java/lang/Number");

	private static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");

	/** Constant for the {@link #math} method. */
	public static final int OR = Opcodes.IOR;

	/** Constant for the {@link #math} method. */
	public static final int REM = Opcodes.IREM;

	/** Constant for the {@link #math} method. */
	public static final int SHL = Opcodes.ISHL;

	private static final Type SHORT_TYPE = Type.getObjectType("java/lang/Short");

	/** Constant for the {@link #math} method. */
	public static final int SHR = Opcodes.ISHR;

	/** Constant for the {@link #math} method. */
	public static final int SUB = Opcodes.ISUB;

	/** Constant for the {@link #math} method. */
	public static final int USHR = Opcodes.IUSHR;

	/** Constant for the {@link #math} method. */
	public static final int XOR = Opcodes.IXOR;

	private static Type getBoxedType(final Type type) {
		switch (type.getSort()) {
		case Type.BYTE:
			return BYTE_TYPE;
		case Type.BOOLEAN:
			return BOOLEAN_TYPE;
		case Type.SHORT:
			return SHORT_TYPE;
		case Type.CHAR:
			return CHARACTER_TYPE;
		case Type.INT:
			return INTEGER_TYPE;
		case Type.FLOAT:
			return FLOAT_TYPE;
		case Type.LONG:
			return LONG_TYPE;
		case Type.DOUBLE:
			return DOUBLE_TYPE;
		default:
			return type;
		}
	}

	/**
	 * Returns the internal names of the given types.
	 *
	 * @param types a set of types.
	 * @return the internal names of the given types.
	 */
	public static String[] getInternalNames(final Type[] types) {
		final String[] names = new String[types.length];
		for (int i = 0; i < names.length; ++i)
			names[i] = types[i].getInternalName();
		return names;
	}

	/** The access flags of the visited method. */
	private final int access;

	/** The argument types of the visited method. */
	public final Type[] argumentTypes;

	/** The types of the local variables of the visited method. */
	private final List<Type> localTypes = new ArrayList<>();

	/** The name of the visited method. */
	private final String name;

	/** The return type of the visited method. */
	private final Type returnType;

	/**
	 * Constructs a new {@link GeneratorAdapter}. <i>Subclasses must not use this
	 * constructor</i>. Instead, they must use the
	 * {@link #GeneratorAdapter(int, MethodVisitor, int, String, String)} version.
	 *
	 * @param access        access flags of the adapted method.
	 * @param method        the adapted method.
	 * @param methodVisitor the method visitor to which this adapter delegates
	 *                      calls.
	 */
	public GeneratorAdapter(final int access, final Method method, final MethodVisitor methodVisitor) {
		this(methodVisitor, access, method.getName(), method.getDescriptor());
	}

	/**
	 * Constructs a new {@link GeneratorAdapter}. <i>Subclasses must not use this
	 * constructor</i>. Instead, they must use the
	 * {@link #GeneratorAdapter(int, MethodVisitor, int, String, String)} version.
	 *
	 * @param access       access flags of the adapted method.
	 * @param method       the adapted method.
	 * @param signature    the signature of the adapted method (may be
	 *                     {@literal null}).
	 * @param exceptions   the exceptions thrown by the adapted method (may be
	 *                     {@literal null}).
	 * @param classVisitor the class visitor to which this adapter delegates calls.
	 */
	public GeneratorAdapter(final int access, final Method method, final String signature, final Type[] exceptions,
			final ClassVisitor classVisitor) {
		this(access, method, classVisitor.visitMethod(access, method.getName(), method.getDescriptor(), signature,
				exceptions == null ? null : getInternalNames(exceptions)));
	}

	/**
	 * Constructs a new {@link GeneratorAdapter}.
	 *
	 * @param api           the ASM API version implemented by this visitor. Must be
	 *                      one of {@link Opcodes#ASM4}, {@link Opcodes#ASM5},
	 *                      {@link Opcodes#ASM6} or {@link Opcodes#ASM7}.
	 * @param methodVisitor the method visitor to which this adapter delegates
	 *                      calls.
	 * @param access        the method's access flags (see {@link Opcodes}).
	 * @param name          the method's name.
	 * @param descriptor    the method's descriptor (see {@link Type}).
	 */
	protected GeneratorAdapter(final int api, final MethodVisitor methodVisitor, final int access, final String name,
			final String descriptor) {
		super(api, access, descriptor, methodVisitor);
		this.access = access;
		this.name = name;
		returnType = Type.getReturnType(descriptor);
		argumentTypes = Type.getArgumentTypes(descriptor);
	}

	/**
	 * Constructs a new {@link GeneratorAdapter}. <i>Subclasses must not use this
	 * constructor</i>. Instead, they must use the
	 * {@link #GeneratorAdapter(int, MethodVisitor, int, String, String)} version.
	 *
	 * @param methodVisitor the method visitor to which this adapter delegates
	 *                      calls.
	 * @param access        the method's access flags (see {@link Opcodes}).
	 * @param name          the method's name.
	 * @param descriptor    the method's descriptor (see {@link Type}).
	 * @throws IllegalStateException if a subclass calls this constructor.
	 */
	public GeneratorAdapter(final MethodVisitor methodVisitor, final int access, final String name,
			final String descriptor) {
		this(Opcodes.ASM7, methodVisitor, access, name, descriptor);
		if (getClass() != GeneratorAdapter.class)
			throw new IllegalStateException();
	}

	/** Generates the instruction to compute the length of an array. */
	public void arrayLength() {
		mv.visitInsn(Opcodes.ARRAYLENGTH);
	}

	/**
	 * Generates the instruction to load an element from an array.
	 *
	 * @param type the type of the array element to be loaded.
	 */
	public void arrayLoad(final Type type) {
		mv.visitInsn(type.getOpcode(Opcodes.IALOAD));
	}

	/**
	 * Generates the instruction to store an element in an array.
	 *
	 * @param type the type of the array element to be stored.
	 */
	public void arrayStore(final Type type) {
		mv.visitInsn(type.getOpcode(Opcodes.IASTORE));
	}

	// -----------------------------------------------------------------------------------------------
	// Instructions to push constants on the stack
	// -----------------------------------------------------------------------------------------------

	/**
	 * Generates the instructions to box the top stack value. This value is replaced
	 * by its boxed equivalent on top of the stack.
	 *
	 * @param type the type of the top stack value.
	 */
	public void box(final Type type) {
		if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY)
			return;
		if (type == Type.VOID_TYPE)
			push((String) null);
		else {
			final Type boxedType = getBoxedType(type);
			newInstance(boxedType);
			if (type.getSize() == 2) {
				// Pp -> Ppo -> oPpo -> ooPpo -> ooPp -> o
				dupX2();
				dupX2();
				pop();
			} else {
				// p -> po -> opo -> oop -> o
				dupX1();
				swap();
			}
			invokeConstructor(boxedType, new Method("<init>", Type.VOID_TYPE, new Type[] { type }));
		}
	}

	/**
	 * Generates the instructions to cast a numerical value from one type to
	 * another.
	 *
	 * @param from the type of the top stack value
	 * @param to   the type into which this value must be cast.
	 */
	public void cast(final Type from, final Type to) {
		if (from != to) {
			if (from.getSort() < Type.BOOLEAN || from.getSort() > Type.DOUBLE || to.getSort() < Type.BOOLEAN
					|| to.getSort() > Type.DOUBLE)
				throw new IllegalArgumentException("Cannot cast from " + from + " to " + to);
			if (from == Type.DOUBLE_TYPE) {
				if (to == Type.FLOAT_TYPE)
					mv.visitInsn(Opcodes.D2F);
				else if (to == Type.LONG_TYPE)
					mv.visitInsn(Opcodes.D2L);
				else {
					mv.visitInsn(Opcodes.D2I);
					cast(Type.INT_TYPE, to);
				}
			} else if (from == Type.FLOAT_TYPE) {
				if (to == Type.DOUBLE_TYPE)
					mv.visitInsn(Opcodes.F2D);
				else if (to == Type.LONG_TYPE)
					mv.visitInsn(Opcodes.F2L);
				else {
					mv.visitInsn(Opcodes.F2I);
					cast(Type.INT_TYPE, to);
				}
			} else if (from == Type.LONG_TYPE) {
				if (to == Type.DOUBLE_TYPE)
					mv.visitInsn(Opcodes.L2D);
				else if (to == Type.FLOAT_TYPE)
					mv.visitInsn(Opcodes.L2F);
				else {
					mv.visitInsn(Opcodes.L2I);
					cast(Type.INT_TYPE, to);
				}
			} else if (to == Type.BYTE_TYPE)
				mv.visitInsn(Opcodes.I2B);
			else if (to == Type.CHAR_TYPE)
				mv.visitInsn(Opcodes.I2C);
			else if (to == Type.DOUBLE_TYPE)
				mv.visitInsn(Opcodes.I2D);
			else if (to == Type.FLOAT_TYPE)
				mv.visitInsn(Opcodes.I2F);
			else if (to == Type.LONG_TYPE)
				mv.visitInsn(Opcodes.I2L);
			else if (to == Type.SHORT_TYPE)
				mv.visitInsn(Opcodes.I2S);
		}
	}

	/**
	 * Marks the start of an exception handler.
	 *
	 * @param start     beginning of the exception handler's scope (inclusive).
	 * @param end       end of the exception handler's scope (exclusive).
	 * @param exception internal name of the type of exceptions handled by the
	 *                  handler.
	 */
	public void catchException(final Label start, final Label end, final Type exception) {
		final Label catchLabel = new Label();
		if (exception == null)
			mv.visitTryCatchBlock(start, end, catchLabel, null);
		else
			mv.visitTryCatchBlock(start, end, catchLabel, exception.getInternalName());
		mark(catchLabel);
	}

	/**
	 * Generates the instruction to check that the top stack value is of the given
	 * type.
	 *
	 * @param type a class or interface type.
	 */
	public void checkCast(final Type type) {
		if (!type.equals(OBJECT_TYPE))
			typeInsn(Opcodes.CHECKCAST, type);
	}

	/** Generates a DUP instruction. */
	public void dup() {
		mv.visitInsn(Opcodes.DUP);
	}

	/** Generates a DUP2 instruction. */
	public void dup2() {
		mv.visitInsn(Opcodes.DUP2);
	}

	/** Generates a DUP2_X1 instruction. */
	public void dup2X1() {
		mv.visitInsn(Opcodes.DUP2_X1);
	}

	/** Generates a DUP2_X2 instruction. */
	public void dup2X2() {
		mv.visitInsn(Opcodes.DUP2_X2);
	}

	/** Generates a DUP_X1 instruction. */
	public void dupX1() {
		mv.visitInsn(Opcodes.DUP_X1);
	}

	// -----------------------------------------------------------------------------------------------
	// Instructions to load and store method arguments
	// -----------------------------------------------------------------------------------------------

	/** Generates a DUP_X2 instruction. */
	public void dupX2() {
		mv.visitInsn(Opcodes.DUP_X2);
	}

	/** Marks the end of the visited method. */
	public void endMethod() {
		if ((access & Opcodes.ACC_ABSTRACT) == 0)
			mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	/**
	 * Generates a get field or set field instruction.
	 *
	 * @param opcode    the instruction's opcode.
	 * @param ownerType the class in which the field is defined.
	 * @param name      the name of the field.
	 * @param fieldType the type of the field.
	 */
	private void fieldInsn(final int opcode, final Type ownerType, final String name, final Type fieldType) {
		mv.visitFieldInsn(opcode, ownerType.getInternalName(), name, fieldType.getDescriptor());
	}

	public int getAccess() {
		return access;
	}

	/**
	 * Returns the index of the given method argument in the frame's local variables
	 * array.
	 *
	 * @param arg the index of a method argument.
	 * @return the index of the given method argument in the frame's local variables
	 *         array.
	 */
	private int getArgIndex(final int arg) {
		int index = (access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
		for (int i = 0; i < arg; i++)
			index += argumentTypes[i].getSize();
		return index;
	}

	public Type[] getArgumentTypes() {
		return argumentTypes.clone();
	}

	/**
	 * Generates the instruction to push the value of a non static field on the
	 * stack.
	 *
	 * @param owner the class in which the field is defined.
	 * @param name  the name of the field.
	 * @param type  the type of the field.
	 */
	public void getField(final Type owner, final String name, final Type type) {
		fieldInsn(Opcodes.GETFIELD, owner, name, type);
	}

	/**
	 * Returns the type of the given local variable.
	 *
	 * @param local a local variable identifier, as returned by
	 *              {@link LocalVariablesSorter#newLocal(Type)}.
	 * @return the type of the given local variable.
	 */
	public Type getLocalType(final int local) {
		return localTypes.get(local - firstLocal);
	}

	public String getName() {
		return name;
	}

	// -----------------------------------------------------------------------------------------------
	// Instructions to load and store local variables
	// -----------------------------------------------------------------------------------------------

	public Type getReturnType() {
		return returnType;
	}

	/**
	 * Generates the instruction to push the value of a static field on the stack.
	 *
	 * @param owner the class in which the field is defined.
	 * @param name  the name of the field.
	 * @param type  the type of the field.
	 */
	public void getStatic(final Type owner, final String name, final Type type) {
		fieldInsn(Opcodes.GETSTATIC, owner, name, type);
	}

	/**
	 * Generates the instruction to jump to the given label.
	 *
	 * @param label where to jump if the condition is {@literal true}.
	 */
	public void goTo(final Label label) {
		mv.visitJumpInsn(Opcodes.GOTO, label);
	}

	/**
	 * Generates the instructions to jump to a label based on the comparison of the
	 * top two stack values.
	 *
	 * @param type  the type of the top two stack values.
	 * @param mode  how these values must be compared. One of EQ, NE, LT, GE, GT,
	 *              LE.
	 * @param label where to jump if the comparison result is {@literal true}.
	 */
	public void ifCmp(final Type type, final int mode, final Label label) {
		switch (type.getSort()) {
		case Type.LONG:
			mv.visitInsn(Opcodes.LCMP);
			break;
		case Type.DOUBLE:
			mv.visitInsn(mode == GE || mode == GT ? Opcodes.DCMPL : Opcodes.DCMPG);
			break;
		case Type.FLOAT:
			mv.visitInsn(mode == GE || mode == GT ? Opcodes.FCMPL : Opcodes.FCMPG);
			break;
		case Type.ARRAY:
		case Type.OBJECT:
			if (mode == EQ) {
				mv.visitJumpInsn(Opcodes.IF_ACMPEQ, label);
				return;
			} else if (mode == NE) {
				mv.visitJumpInsn(Opcodes.IF_ACMPNE, label);
				return;
			} else
				throw new IllegalArgumentException("Bad comparison for type " + type);
		default:
			int intOp = -1;
			switch (mode) {
			case EQ:
				intOp = Opcodes.IF_ICMPEQ;
				break;
			case NE:
				intOp = Opcodes.IF_ICMPNE;
				break;
			case GE:
				intOp = Opcodes.IF_ICMPGE;
				break;
			case LT:
				intOp = Opcodes.IF_ICMPLT;
				break;
			case LE:
				intOp = Opcodes.IF_ICMPLE;
				break;
			case GT:
				intOp = Opcodes.IF_ICMPGT;
				break;
			default:
				throw new IllegalArgumentException("Bad comparison mode " + mode);
			}
			mv.visitJumpInsn(intOp, label);
			return;
		}
		mv.visitJumpInsn(mode, label);
	}

	/**
	 * Generates the instructions to jump to a label based on the comparison of the
	 * top two integer stack values.
	 *
	 * @param mode  how these values must be compared. One of EQ, NE, LT, GE, GT,
	 *              LE.
	 * @param label where to jump if the comparison result is {@literal true}.
	 */
	public void ifICmp(final int mode, final Label label) {
		ifCmp(Type.INT_TYPE, mode, label);
	}

	/**
	 * Generates the instruction to jump to the given label if the top stack value
	 * is not null.
	 *
	 * @param label where to jump if the condition is {@literal true}.
	 */
	public void ifNonNull(final Label label) {
		mv.visitJumpInsn(Opcodes.IFNONNULL, label);
	}

	/**
	 * Generates the instruction to jump to the given label if the top stack value
	 * is null.
	 *
	 * @param label where to jump if the condition is {@literal true}.
	 */
	public void ifNull(final Label label) {
		mv.visitJumpInsn(Opcodes.IFNULL, label);
	}

	/**
	 * Generates the instructions to jump to a label based on the comparison of the
	 * top integer stack value with zero.
	 *
	 * @param mode  how these values must be compared. One of EQ, NE, LT, GE, GT,
	 *              LE.
	 * @param label where to jump if the comparison result is {@literal true}.
	 */
	public void ifZCmp(final int mode, final Label label) {
		mv.visitJumpInsn(mode, label);
	}

	// -----------------------------------------------------------------------------------------------
	// Instructions to manage the stack
	// -----------------------------------------------------------------------------------------------

	/**
	 * Generates the instruction to increment the given local variable.
	 *
	 * @param local  the local variable to be incremented.
	 * @param amount the amount by which the local variable must be incremented.
	 */
	public void iinc(final int local, final int amount) {
		mv.visitIincInsn(local, amount);
	}

	/**
	 * Generates the instruction to test if the top stack value is of the given
	 * type.
	 *
	 * @param type a class or interface type.
	 */
	public void instanceOf(final Type type) {
		typeInsn(Opcodes.INSTANCEOF, type);
	}

	/**
	 * Generates the instruction to invoke a constructor.
	 *
	 * @param type   the class in which the constructor is defined.
	 * @param method the constructor to be invoked.
	 */
	public void invokeConstructor(final Type type, final Method method) {
		invokeInsn(Opcodes.INVOKESPECIAL, type, method, false);
	}

	/**
	 * Generates an invokedynamic instruction.
	 *
	 * @param name                     the method's name.
	 * @param descriptor               the method's descriptor (see {@link Type}).
	 * @param bootstrapMethodHandle    the bootstrap method.
	 * @param bootstrapMethodArguments the bootstrap method constant arguments. Each
	 *                                 argument must be an {@link Integer},
	 *                                 {@link Float}, {@link Long}, {@link Double},
	 *                                 {@link String}, {@link Type} or
	 *                                 {@link Handle} value. This method is allowed
	 *                                 to modify the content of the array so a
	 *                                 caller should expect that this array may
	 *                                 change.
	 */
	public void invokeDynamic(final String name, final String descriptor, final Handle bootstrapMethodHandle,
			final Object... bootstrapMethodArguments) {
		mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
	}

	/**
	 * Generates an invoke method instruction.
	 *
	 * @param opcode      the instruction's opcode.
	 * @param type        the class in which the method is defined.
	 * @param method      the method to be invoked.
	 * @param isInterface whether the 'type' class is an interface or not.
	 */
	private void invokeInsn(final int opcode, final Type type, final Method method, final boolean isInterface) {
		final String owner = type.getSort() == Type.ARRAY ? type.getDescriptor() : type.getInternalName();
		mv.visitMethodInsn(opcode, owner, method.getName(), method.getDescriptor(), isInterface);
	}

	/**
	 * Generates the instruction to invoke an interface method.
	 *
	 * @param owner  the class in which the method is defined.
	 * @param method the method to be invoked.
	 */
	public void invokeInterface(final Type owner, final Method method) {
		invokeInsn(Opcodes.INVOKEINTERFACE, owner, method, true);
	}

	/**
	 * Generates the instruction to invoke a static method.
	 *
	 * @param owner  the class in which the method is defined.
	 * @param method the method to be invoked.
	 */
	public void invokeStatic(final Type owner, final Method method) {
		invokeInsn(Opcodes.INVOKESTATIC, owner, method, false);
	}

	/**
	 * Generates the instruction to invoke a normal method.
	 *
	 * @param owner  the class in which the method is defined.
	 * @param method the method to be invoked.
	 */
	public void invokeVirtual(final Type owner, final Method method) {
		invokeInsn(Opcodes.INVOKEVIRTUAL, owner, method, false);
	}

	/**
	 * Generates the instruction to load the given method argument on the stack.
	 *
	 * @param arg the index of a method argument.
	 */
	public void loadArg(final int arg) {
		loadInsn(argumentTypes[arg], getArgIndex(arg));
	}

	/**
	 * Generates the instructions to load all the method arguments on the stack, as
	 * a single object array.
	 */
	public void loadArgArray() {
		push(argumentTypes.length);
		newArray(OBJECT_TYPE);
		for (int i = 0; i < argumentTypes.length; i++) {
			dup();
			push(i);
			loadArg(i);
			box(argumentTypes[i]);
			arrayStore(OBJECT_TYPE);
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Instructions to do mathematical and logical operations
	// -----------------------------------------------------------------------------------------------

	/** Generates the instructions to load all the method arguments on the stack. */
	public void loadArgs() {
		loadArgs(0, argumentTypes.length);
	}

	/**
	 * Generates the instructions to load the given method arguments on the stack.
	 *
	 * @param arg   the index of the first method argument to be loaded.
	 * @param count the number of method arguments to be loaded.
	 */
	public void loadArgs(final int arg, final int count) {
		int index = getArgIndex(arg);
		for (int i = 0; i < count; ++i) {
			final Type argumentType = argumentTypes[arg + i];
			loadInsn(argumentType, index);
			index += argumentType.getSize();
		}
	}

	/**
	 * Generates the instruction to push a local variable on the stack.
	 *
	 * @param type  the type of the local variable to be loaded.
	 * @param index an index in the frame's local variables array.
	 */
	private void loadInsn(final Type type, final int index) {
		mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
	}

	/**
	 * Generates the instruction to load the given local variable on the stack.
	 *
	 * @param local a local variable identifier, as returned by
	 *              {@link LocalVariablesSorter#newLocal(Type)}.
	 */
	public void loadLocal(final int local) {
		loadInsn(getLocalType(local), local);
	}

	// -----------------------------------------------------------------------------------------------
	// Instructions to do boxing and unboxing operations
	// -----------------------------------------------------------------------------------------------

	/**
	 * Generates the instruction to load the given local variable on the stack.
	 *
	 * @param local a local variable identifier, as returned by
	 *              {@link LocalVariablesSorter#newLocal(Type)}.
	 * @param type  the type of this local variable.
	 */
	public void loadLocal(final int local, final Type type) {
		setLocalType(local, type);
		loadInsn(type, local);
	}

	/** Generates the instruction to load 'this' on the stack. */
	public void loadThis() {
		if ((access & Opcodes.ACC_STATIC) != 0)
			throw new IllegalStateException("no 'this' pointer within static method");
		mv.visitVarInsn(Opcodes.ALOAD, 0);
	}

	/**
	 * Marks the current code position with a new label.
	 *
	 * @return the label that was created to mark the current code position.
	 */
	public Label mark() {
		final Label label = new Label();
		mv.visitLabel(label);
		return label;
	}

	/**
	 * Marks the current code position with the given label.
	 *
	 * @param label a label.
	 */
	public void mark(final Label label) {
		mv.visitLabel(label);
	}

	// -----------------------------------------------------------------------------------------------
	// Instructions to jump to other instructions
	// -----------------------------------------------------------------------------------------------

	/**
	 * Generates the instruction to do the specified mathematical or logical
	 * operation.
	 *
	 * @param op   a mathematical or logical operation. Must be one of ADD, SUB,
	 *             MUL, DIV, REM, NEG, SHL, SHR, USHR, AND, OR, XOR.
	 * @param type the type of the operand(s) for this operation.
	 */
	public void math(final int op, final Type type) {
		mv.visitInsn(type.getOpcode(op));
	}

	/** Generates the instruction to get the monitor of the top stack value. */
	public void monitorEnter() {
		mv.visitInsn(Opcodes.MONITORENTER);
	}

	/** Generates the instruction to release the monitor of the top stack value. */
	public void monitorExit() {
		mv.visitInsn(Opcodes.MONITOREXIT);
	}

	/**
	 * Generates the instruction to create a new array.
	 *
	 * @param type the type of the array elements.
	 */
	public void newArray(final Type type) {
		int arrayType;
		switch (type.getSort()) {
		case Type.BOOLEAN:
			arrayType = Opcodes.T_BOOLEAN;
			break;
		case Type.CHAR:
			arrayType = Opcodes.T_CHAR;
			break;
		case Type.BYTE:
			arrayType = Opcodes.T_BYTE;
			break;
		case Type.SHORT:
			arrayType = Opcodes.T_SHORT;
			break;
		case Type.INT:
			arrayType = Opcodes.T_INT;
			break;
		case Type.FLOAT:
			arrayType = Opcodes.T_FLOAT;
			break;
		case Type.LONG:
			arrayType = Opcodes.T_LONG;
			break;
		case Type.DOUBLE:
			arrayType = Opcodes.T_DOUBLE;
			break;
		default:
			typeInsn(Opcodes.ANEWARRAY, type);
			return;
		}
		mv.visitIntInsn(Opcodes.NEWARRAY, arrayType);
	}

	/**
	 * Generates the instruction to create a new object.
	 *
	 * @param type the class of the object to be created.
	 */
	public void newInstance(final Type type) {
		typeInsn(Opcodes.NEW, type);
	}

	/**
	 * Constructs a new {@link Label}.
	 *
	 * @return a new {@link Label}.
	 */
	public Label newLabel() {
		return new Label();
	}

	/**
	 * Generates the instructions to compute the bitwise negation of the top stack
	 * value.
	 */
	public void not() {
		mv.visitInsn(Opcodes.ICONST_1);
		mv.visitInsn(Opcodes.IXOR);
	}

	/** Generates a POP instruction. */
	public void pop() {
		mv.visitInsn(Opcodes.POP);
	}

	/** Generates a POP2 instruction. */
	public void pop2() {
		mv.visitInsn(Opcodes.POP2);
	}

	/**
	 * Generates the instruction to push the given value on the stack.
	 *
	 * @param value the value to be pushed on the stack.
	 */
	public void push(final boolean value) {
		push(value ? 1 : 0);
	}

	/**
	 * Generates the instruction to push a constant dynamic on the stack.
	 *
	 * @param constantDynamic the constant dynamic to be pushed on the stack.
	 */
	public void push(final ConstantDynamic constantDynamic) {
		if (constantDynamic == null)
			mv.visitInsn(Opcodes.ACONST_NULL);
		else
			mv.visitLdcInsn(constantDynamic);
	}

	// -----------------------------------------------------------------------------------------------
	// Instructions to load and store fields
	// -----------------------------------------------------------------------------------------------

	/**
	 * Generates the instruction to push the given value on the stack.
	 *
	 * @param value the value to be pushed on the stack.
	 */
	public void push(final double value) {
		final long bits = Double.doubleToLongBits(value);
		if (bits == 0L || bits == 0x3FF0000000000000L)
			mv.visitInsn(Opcodes.DCONST_0 + (int) value);
		else
			mv.visitLdcInsn(value);
	}

	/**
	 * Generates the instruction to push the given value on the stack.
	 *
	 * @param value the value to be pushed on the stack.
	 */
	public void push(final float value) {
		final int bits = Float.floatToIntBits(value);
		if (bits == 0L || bits == 0x3F800000 || bits == 0x40000000)
			mv.visitInsn(Opcodes.FCONST_0 + (int) value);
		else
			mv.visitLdcInsn(value);
	}

	/**
	 * Generates the instruction to push a handle on the stack.
	 *
	 * @param handle the handle to be pushed on the stack.
	 */
	public void push(final Handle handle) {
		if (handle == null)
			mv.visitInsn(Opcodes.ACONST_NULL);
		else
			mv.visitLdcInsn(handle);
	}

	/**
	 * Generates the instruction to push the given value on the stack.
	 *
	 * @param value the value to be pushed on the stack.
	 */
	public void push(final int value) {
		if (value >= -1 && value <= 5)
			mv.visitInsn(Opcodes.ICONST_0 + value);
		else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE)
			mv.visitIntInsn(Opcodes.BIPUSH, value);
		else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE)
			mv.visitIntInsn(Opcodes.SIPUSH, value);
		else
			mv.visitLdcInsn(value);
	}

	/**
	 * Generates the instruction to push the given value on the stack.
	 *
	 * @param value the value to be pushed on the stack.
	 */
	public void push(final long value) {
		if (value == 0L || value == 1L)
			mv.visitInsn(Opcodes.LCONST_0 + (int) value);
		else
			mv.visitLdcInsn(value);
	}

	// -----------------------------------------------------------------------------------------------
	// Instructions to invoke methods
	// -----------------------------------------------------------------------------------------------

	/**
	 * Generates the instruction to push the given value on the stack.
	 *
	 * @param value the value to be pushed on the stack. May be {@literal null}.
	 */
	public void push(final String value) {
		if (value == null)
			mv.visitInsn(Opcodes.ACONST_NULL);
		else
			mv.visitLdcInsn(value);
	}

	/**
	 * Generates the instruction to push the given value on the stack.
	 *
	 * @param value the value to be pushed on the stack.
	 */
	public void push(final Type value) {
		if (value == null)
			mv.visitInsn(Opcodes.ACONST_NULL);
		else
			switch (value.getSort()) {
			case Type.BOOLEAN:
				mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Boolean", "TYPE", CLASS_DESCRIPTOR);
				break;
			case Type.CHAR:
				mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Character", "TYPE", CLASS_DESCRIPTOR);
				break;
			case Type.BYTE:
				mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Byte", "TYPE", CLASS_DESCRIPTOR);
				break;
			case Type.SHORT:
				mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Short", "TYPE", CLASS_DESCRIPTOR);
				break;
			case Type.INT:
				mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Integer", "TYPE", CLASS_DESCRIPTOR);
				break;
			case Type.FLOAT:
				mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Float", "TYPE", CLASS_DESCRIPTOR);
				break;
			case Type.LONG:
				mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Long", "TYPE", CLASS_DESCRIPTOR);
				break;
			case Type.DOUBLE:
				mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Double", "TYPE", CLASS_DESCRIPTOR);
				break;
			default:
				mv.visitLdcInsn(value);
				break;
			}
	}

	/**
	 * Generates the instruction to store the top stack value in a non static field.
	 *
	 * @param owner the class in which the field is defined.
	 * @param name  the name of the field.
	 * @param type  the type of the field.
	 */
	public void putField(final Type owner, final String name, final Type type) {
		fieldInsn(Opcodes.PUTFIELD, owner, name, type);
	}

	/**
	 * Generates the instruction to store the top stack value in a static field.
	 *
	 * @param owner the class in which the field is defined.
	 * @param name  the name of the field.
	 * @param type  the type of the field.
	 */
	public void putStatic(final Type owner, final String name, final Type type) {
		fieldInsn(Opcodes.PUTSTATIC, owner, name, type);
	}

	/**
	 * Generates a RET instruction.
	 *
	 * @param local a local variable identifier, as returned by
	 *              {@link LocalVariablesSorter#newLocal(Type)}.
	 */
	public void ret(final int local) {
		mv.visitVarInsn(Opcodes.RET, local);
	}

	/** Generates the instruction to return the top stack value to the caller. */
	public void returnValue() {
		mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
	}

	// -----------------------------------------------------------------------------------------------
	// Instructions to create objects and arrays
	// -----------------------------------------------------------------------------------------------

	@Override
	protected void setLocalType(final int local, final Type type) {
		final int index = local - firstLocal;
		while (localTypes.size() < index + 1)
			localTypes.add(null);
		localTypes.set(index, type);
	}

	/**
	 * Generates the instruction to store the top stack value in the given method
	 * argument.
	 *
	 * @param arg the index of a method argument.
	 */
	public void storeArg(final int arg) {
		storeInsn(argumentTypes[arg], getArgIndex(arg));
	}

	/**
	 * Generates the instruction to store the top stack value in a local variable.
	 *
	 * @param type  the type of the local variable to be stored.
	 * @param index an index in the frame's local variables array.
	 */
	private void storeInsn(final Type type, final int index) {
		mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), index);
	}

	// -----------------------------------------------------------------------------------------------
	// Miscellaneous instructions
	// -----------------------------------------------------------------------------------------------

	/**
	 * Generates the instruction to store the top stack value in the given local
	 * variable.
	 *
	 * @param local a local variable identifier, as returned by
	 *              {@link LocalVariablesSorter#newLocal(Type)}.
	 */
	public void storeLocal(final int local) {
		storeInsn(getLocalType(local), local);
	}

	/**
	 * Generates the instruction to store the top stack value in the given local
	 * variable.
	 *
	 * @param local a local variable identifier, as returned by
	 *              {@link LocalVariablesSorter#newLocal(Type)}.
	 * @param type  the type of this local variable.
	 */
	public void storeLocal(final int local, final Type type) {
		setLocalType(local, type);
		storeInsn(type, local);
	}

	/** Generates a SWAP instruction. */
	public void swap() {
		mv.visitInsn(Opcodes.SWAP);
	}

	/**
	 * Generates the instructions to swap the top two stack values.
	 *
	 * @param prev type of the top - 1 stack value.
	 * @param type type of the top stack value.
	 */
	public void swap(final Type prev, final Type type) {
		if (type.getSize() == 1) {
			if (prev.getSize() == 1)
				swap(); // Same as dupX1 pop.
			else {
				dupX2();
				pop();
			}
		} else if (prev.getSize() == 1) {
			dup2X1();
			pop2();
		} else {
			dup2X2();
			pop2();
		}
	}

	/** Generates the instruction to throw an exception. */
	public void throwException() {
		mv.visitInsn(Opcodes.ATHROW);
	}

	/**
	 * Generates the instructions to create and throw an exception. The exception
	 * class must have a constructor with a single String argument.
	 *
	 * @param type    the class of the exception to be thrown.
	 * @param message the detailed message of the exception.
	 */
	public void throwException(final Type type, final String message) {
		newInstance(type);
		dup();
		push(message);
		invokeConstructor(type, Method.getMethod("void <init> (String)"));
		throwException();
	}

	/**
	 * Generates a type dependent instruction.
	 *
	 * @param opcode the instruction's opcode.
	 * @param type   the instruction's operand.
	 */
	private void typeInsn(final int opcode, final Type type) {
		mv.visitTypeInsn(opcode, type.getInternalName());
	}

	// -----------------------------------------------------------------------------------------------
	// Non instructions
	// -----------------------------------------------------------------------------------------------

	/**
	 * Generates the instructions to unbox the top stack value. This value is
	 * replaced by its unboxed equivalent on top of the stack.
	 *
	 * @param type the type of the top stack value.
	 */
	public void unbox(final Type type) {
		Type boxedType = NUMBER_TYPE;
		Method unboxMethod;
		switch (type.getSort()) {
		case Type.VOID:
			return;
		case Type.CHAR:
			boxedType = CHARACTER_TYPE;
			unboxMethod = CHAR_VALUE;
			break;
		case Type.BOOLEAN:
			boxedType = BOOLEAN_TYPE;
			unboxMethod = BOOLEAN_VALUE;
			break;
		case Type.DOUBLE:
			unboxMethod = DOUBLE_VALUE;
			break;
		case Type.FLOAT:
			unboxMethod = FLOAT_VALUE;
			break;
		case Type.LONG:
			unboxMethod = LONG_VALUE;
			break;
		case Type.INT:
		case Type.SHORT:
		case Type.BYTE:
			unboxMethod = INT_VALUE;
			break;
		default:
			unboxMethod = null;
			break;
		}
		if (unboxMethod == null)
			checkCast(type);
		else {
			checkCast(boxedType);
			invokeVirtual(boxedType, unboxMethod);
		}
	}

	/**
	 * Generates the instructions to box the top stack value using Java 5's
	 * valueOf() method. This value is replaced by its boxed equivalent on top of
	 * the stack.
	 *
	 * @param type the type of the top stack value.
	 */
	public void valueOf(final Type type) {
		if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY)
			return;
		if (type == Type.VOID_TYPE)
			push((String) null);
		else {
			final Type boxedType = getBoxedType(type);
			invokeStatic(boxedType, new Method("valueOf", boxedType, new Type[] { type }));
		}
	}
}
