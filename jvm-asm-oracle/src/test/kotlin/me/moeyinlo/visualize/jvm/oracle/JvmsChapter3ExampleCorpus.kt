package me.moeyinlo.visualize.jvm.oracle

data class JvmsChapter3Example(
    val name: String,
    val sourceName: String,
    val internalName: String,
    val chapter3Topics: List<String>,
    val source: String,
)

object JvmsChapter3ExampleCorpus {
    val examples: List<JvmsChapter3Example> = listOf(
        JvmsChapter3Example(
            name = "arithmetic and local variables",
            sourceName = "chapter3/ArithmeticAndLocals.java",
            internalName = "chapter3/ArithmeticAndLocals",
            chapter3Topics = listOf("local variables", "operand stack", "integer arithmetic", "narrowing conversion"),
            source = """
                package chapter3;

                public class ArithmeticAndLocals {
                    public int compute(int a, int b) {
                        int sum = a + b;
                        int product = sum * 7;
                        return (byte) product;
                    }
                }
            """.trimIndent(),
        ),
        JvmsChapter3Example(
            name = "control flow and switch",
            sourceName = "chapter3/ControlFlowAndSwitch.java",
            internalName = "chapter3/ControlFlowAndSwitch",
            chapter3Topics = listOf("if", "while", "tableswitch/lookupswitch", "branches"),
            source = """
                package chapter3;

                public class ControlFlowAndSwitch {
                    public int score(int input) {
                        int total = 0;
                        int cursor = input;
                        while (cursor > 0) {
                            total += cursor;
                            cursor--;
                        }
                        switch (input) {
                            case 1:
                                return total + 10;
                            case 7:
                                return total + 70;
                            default:
                                return total;
                        }
                    }
                }
            """.trimIndent(),
        ),
        JvmsChapter3Example(
            name = "method invocation and object construction",
            sourceName = "chapter3/InvocationAndConstruction.java",
            internalName = "chapter3/InvocationAndConstruction",
            chapter3Topics = listOf("new", "invokespecial", "invokevirtual", "invokestatic", "fields"),
            source = """
                package chapter3;

                public class InvocationAndConstruction {
                    private final int value;

                    public InvocationAndConstruction(int value) {
                        this.value = value;
                    }

                    public int instanceValue() {
                        return value;
                    }

                    public static int makeAndRead(int value) {
                        InvocationAndConstruction constructed = new InvocationAndConstruction(value);
                        return constructed.instanceValue();
                    }
                }
            """.trimIndent(),
        ),
        JvmsChapter3Example(
            name = "arrays",
            sourceName = "chapter3/ArrayOperations.java",
            internalName = "chapter3/ArrayOperations",
            chapter3Topics = listOf("newarray", "anewarray", "arraylength", "xaload", "xastore"),
            source = """
                package chapter3;

                public class ArrayOperations {
                    public int sumFirstTwo(String label) {
                        int[] values = new int[] { 3, 4, label.length() };
                        String[] labels = new String[] { label, "fallback" };
                        return values[0] + values[1] + labels.length;
                    }
                }
            """.trimIndent(),
        ),
        JvmsChapter3Example(
            name = "exceptions and finally",
            sourceName = "chapter3/ExceptionsAndFinally.java",
            internalName = "chapter3/ExceptionsAndFinally",
            chapter3Topics = listOf("athrow", "exception table", "try-catch", "finally"),
            source = """
                package chapter3;

                public class ExceptionsAndFinally {
                    public int guard(String value) {
                        int cleanup = 1;
                        try {
                            if (value == null) {
                                throw new IllegalArgumentException("value");
                            }
                            return value.length();
                        } catch (IllegalArgumentException exception) {
                            return -1;
                        } finally {
                            cleanup++;
                        }
                    }
                }
            """.trimIndent(),
        ),
        JvmsChapter3Example(
            name = "synchronization",
            sourceName = "chapter3/SynchronizationExample.java",
            internalName = "chapter3/SynchronizationExample",
            chapter3Topics = listOf("monitorenter", "monitorexit", "synchronized method"),
            source = """
                package chapter3;

                public class SynchronizationExample {
                    private int value;

                    public synchronized int increment() {
                        value++;
                        return value;
                    }

                    public int guarded(Object lock) {
                        synchronized (lock) {
                            return value;
                        }
                    }
                }
            """.trimIndent(),
        ),
    )
}
