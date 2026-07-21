package me.moeyinlo.visualize.jvm.oracle

data class HotSpotRuntimeDifferentialCase(
    val name: String,
    val sourceName: String,
    val internalName: String,
    val entryMethodName: String,
    val expectedResult: Any,
    val source: String,
)

object HotSpotRuntimeDifferentialCorpus {
    val cases: List<HotSpotRuntimeDifferentialCase> = listOf(
        HotSpotRuntimeDifferentialCase(
            name = "integer loop arithmetic",
            sourceName = "hotspot/IntegerLoopArithmetic.java",
            internalName = "hotspot/IntegerLoopArithmetic",
            entryMethodName = "entry",
            expectedResult = 55,
            source = """
                package hotspot;

                public class IntegerLoopArithmetic {
                    public static int entry() {
                        int total = 0;
                        for (int i = 1; i <= 10; i++) {
                            total += i;
                        }
                        return total;
                    }
                }
            """.trimIndent(),
        ),
        HotSpotRuntimeDifferentialCase(
            name = "string switch dispatch",
            sourceName = "hotspot/StringSwitchDispatch.java",
            internalName = "hotspot/StringSwitchDispatch",
            entryMethodName = "entry",
            expectedResult = "beta",
            source = """
                package hotspot;

                public class StringSwitchDispatch {
                    public static String entry() {
                        String value = "b";
                        switch (value) {
                            case "a":
                                return "alpha";
                            case "b":
                                return "beta";
                            default:
                                return "other";
                        }
                    }
                }
            """.trimIndent(),
        ),
        HotSpotRuntimeDifferentialCase(
            name = "array allocation and stores",
            sourceName = "hotspot/ArrayAllocationAndStores.java",
            internalName = "hotspot/ArrayAllocationAndStores",
            entryMethodName = "entry",
            expectedResult = 14,
            source = """
                package hotspot;

                public class ArrayAllocationAndStores {
                    public static int entry() {
                        int[] values = new int[3];
                        values[0] = 2;
                        values[1] = 5;
                        values[2] = 7;
                        return values[0] + values[1] + values[2];
                    }
                }
            """.trimIndent(),
        ),
        HotSpotRuntimeDifferentialCase(
            name = "exception handler result",
            sourceName = "hotspot/ExceptionHandlerResult.java",
            internalName = "hotspot/ExceptionHandlerResult",
            entryMethodName = "entry",
            expectedResult = "caught",
            source = """
                package hotspot;

                public class ExceptionHandlerResult {
                    public static String entry() {
                        try {
                            throw new IllegalStateException("boom");
                        } catch (IllegalStateException exception) {
                            return "caught";
                        }
                    }
                }
            """.trimIndent(),
        ),
        HotSpotRuntimeDifferentialCase(
            name = "synchronized static method",
            sourceName = "hotspot/SynchronizedStaticMethod.java",
            internalName = "hotspot/SynchronizedStaticMethod",
            entryMethodName = "entry",
            expectedResult = 2,
            source = """
                package hotspot;

                public class SynchronizedStaticMethod {
                    private static int value = 1;

                    public static synchronized int entry() {
                        value += 1;
                        return value;
                    }
                }
            """.trimIndent(),
        ),
    )
}
