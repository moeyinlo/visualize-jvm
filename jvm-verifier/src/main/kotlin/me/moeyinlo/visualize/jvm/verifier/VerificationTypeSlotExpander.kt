package me.moeyinlo.visualize.jvm.verifier

object VerificationTypeSlotExpander {
    fun expand(types: List<VerificationType>): List<VerificationType> =
        types.flatMap { type ->
            if (type.locationCount == 2) {
                listOf(type, VerificationType.Top)
            } else {
                listOf(type)
            }
        }
}
