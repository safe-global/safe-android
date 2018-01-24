package pm.gnosis.mnemonic

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import pm.gnosis.tests.utils.Asserts.assertThrow
import pm.gnosis.utils.toHexString
import pm.gnosis.utils.words

class Bip39GeneratorTest {
    private lateinit var bip39: Bip39Generator

    private val testWorldListProvider = TestWorldListProvider()

    @Before
    fun setup() {
        bip39 = Bip39Generator(testWorldListProvider)
    }

    @Test
    fun mnemonicToSeed() {
        MNEMONIC_TO_SEED.forEach { mnemonic, seed ->
            assertEquals(seed, bip39.mnemonicToSeed(mnemonic).toHexString())
        }
    }

    @Test
    fun testGenerateMnemonic() {
        TestWorldListProvider.Companion.MAP.forEach { (key, _) ->
            val mnemonic12 = bip39.generateMnemonic(languageId = key)
            assertEquals(12, mnemonic12.words().size)
            assertEquals(mnemonic12, bip39.validateMnemonic(mnemonic12))

            val mnemonic15 = bip39.generateMnemonic(Bip39.MIN_ENTROPY_BITS + 1 * Bip39.ENTROPY_MULTIPLE, key)
            assertEquals(15, mnemonic15.words().size)
            assertEquals(mnemonic15, bip39.validateMnemonic(mnemonic15))

            val mnemonic18 = bip39.generateMnemonic(Bip39.MIN_ENTROPY_BITS + 2 * Bip39.ENTROPY_MULTIPLE, key)
            assertEquals(18, mnemonic18.words().size)
            assertEquals(mnemonic18, bip39.validateMnemonic(mnemonic18))

            val mnemonic21 = bip39.generateMnemonic(Bip39.MIN_ENTROPY_BITS + 3 * Bip39.ENTROPY_MULTIPLE, key)
            assertEquals(21, mnemonic21.words().size)
            assertEquals(mnemonic21, bip39.validateMnemonic(mnemonic21))

            val mnemonic24 = bip39.generateMnemonic(Bip39.MAX_ENTROPY_BITS, key)
            assertEquals(24, mnemonic24.words().size)
            assertEquals(mnemonic24, bip39.validateMnemonic(mnemonic24))
        }
    }

    @Test
    fun generateMnemonicInvalidEntropy() {
        assertThrow({ bip39.generateMnemonic(0, TestWorldListProvider.ENGLISH) }, throwablePredicate = { it is IllegalArgumentException })
        assertThrow({ bip39.generateMnemonic(127, TestWorldListProvider.ENGLISH) }, throwablePredicate = { it is IllegalArgumentException })
        assertThrow({ bip39.generateMnemonic(129, TestWorldListProvider.ENGLISH) }, throwablePredicate = { it is IllegalArgumentException })
        assertThrow({ bip39.generateMnemonic(257, TestWorldListProvider.ENGLISH) }, throwablePredicate = { it is IllegalArgumentException })
    }

    @Test
    fun testValidateMnemonic() {
        (MNEMONICS_ENGLISH_12 + MNEMONICS_ENGLISH_15 + MNEMONICS_ENGLISH_18 + MNEMONICS_ENGLISH_21 +
                MNEMONICS_ENGLISH_24).forEach {
            assertEquals(it, bip39.validateMnemonic(it))
        }
    }

    @Test
    fun testInvalidMnemonics() {
        (INVALID_MNEMONICS_ENGLISH_12 + INVALID_MNEMONICS_ENGLISH_15 + INVALID_MNEMONICS_ENGLISH_18 +
                INVALID_MNEMONICS_ENGLISH_21 + INVALID_MNEMONICS_ENGLISH_24).forEach {
            try {
                bip39.validateMnemonic(it)
                fail()
            } catch (ignored: Exception) {
            }
        }
    }

    @Test
    fun validateEmptyMnemonic() {
        assertThrow({ bip39.validateMnemonic("") }, throwablePredicate = { it is EmptyMnemonic })
        assertThrow({ bip39.validateMnemonic("    ") }, throwablePredicate = { it is EmptyMnemonic })
        assertThrow({ bip39.validateMnemonic("\n") }, throwablePredicate = { it is EmptyMnemonic })
        assertThrow({ bip39.validateMnemonic("\t") }, throwablePredicate = { it is EmptyMnemonic })
    }

    @Test
    fun validateInvalidEntropy() {
        assertThrow({ bip39.validateMnemonic("page") },
                throwablePredicate = { it == InvalidEntropy("page", 0) })
        assertThrow({ bip39.validateMnemonic("page tuition excess kidney") },
                throwablePredicate = { it == InvalidEntropy("page tuition excess kidney", 32) })
        assertThrow({ bip39.validateMnemonic("page tuition excess kidney forward collect fashion finger raven honey tiny") },
                throwablePredicate = { it == InvalidEntropy("page tuition excess kidney forward collect fashion finger raven honey tiny", 96) })
    }

    @Test
    fun validateMnemonicWithInvalidWord() {
        assertThrow({ bip39.validateMnemonic("page tuition excess kidney forward collect fashion finger raven honey tiny gnosis") },
                throwablePredicate = { it is MnemonicNotInWordlist })
    }

    @Test
    fun generateMnemonicWithInvalidLanguageId() {
        assertThrow({ bip39.generateMnemonic(languageId = -1) }, throwablePredicate = { it is IllegalArgumentException })
    }

    companion object {
        private val MNEMONICS_ENGLISH_12 = listOf(
                "page tuition excess kidney forward collect fashion finger raven honey tiny wall",
                "moon all public fame skin paper typical dad balance chicken correct chicken",
                "strike hill deposit bus globe recall good direct acquire nerve hammer pizza",
                "cattle citizen wing liar tank avoid clean cause error purse pave despair")

        private val MNEMONICS_ENGLISH_15 = listOf(
                "prison riot common gloom claim detect afraid tunnel treat people mean rule pluck use flush",
                "good behind napkin region maple ghost fold spare club tunnel social fit between fly weasel",
                "waste pause patient achieve cook lobster peasant snake mountain mixed mushroom soda system steak mask",
                "zone apart enable urge member deputy impose wrong scene talk soon arctic warrior polar ostrich")

        private val MNEMONICS_ENGLISH_18 = listOf(
                "silly remind knee arrow behind admit moon impact grab start curious fabric junior empty insect wood pool icon",
                "clean pottery produce voyage humor hello guide panther myself keep dynamic glow total flight right talk alone deposit",
                "visit cabin pattern rent buyer journey prefer comfort flush give icon doctor annual power pumpkin shoe pave couch",
                "float lab gadget sea trick toast series chaos universe boss seat spice cabbage horn glimpse display soup fit")

        private val MNEMONICS_ENGLISH_21 = listOf(
                "discover divert waste tape purse travel mystery liquid vacuum stay eager breeze toilet fortune dad click earth hair skate trigger dentist",
                "treat gown begin enjoy hero curious manage survey outside earn genius secret upset bread season dawn action maple more paddle sentence",
                "sail illegal deer fly biology aerobic clarify drift symbol click reopen aisle present aspect shoot home hover moon clerk vehicle hidden",
                "small tonight umbrella chest replace often bike become short enough match reject miracle vicious interest tip infant breeze depend review interest",
                "shallow captain now awkward siege census forget guard soup shoot learn spider scan maid explain digital decrease siege mushroom above title")

        private val MNEMONICS_ENGLISH_24 = listOf(
                "neglect interest loyal cactus better evil angle resist useful fashion argue supply around wife trash quote second shift trap regret owner kitten peanut ivory",
                "crack gather slot leisure actress solve raw dash increase seven sense castle veteran moon struggle fortune only chair jaguar march arctic rug suffer pyramid",
                "dove horse distance sand blood fire oak target mimic session direct donor assume dog method sadness acid oven time boost sting regular turkey key",
                "favorite split mammal bubble pluck clutch dragon method stage bullet tornado base ginger captain chuckle myself dynamic advice wish scale skin drum spray demand")

        private val INVALID_MNEMONICS_ENGLISH_12 = listOf(
                "page fame excess kidney forward collect fashion finger raven honey tiny wall",
                "moon all public fame skin collect typical dad balance chicken correct chicken",
                "strike hill deposit bus globe recall fame direct acquire nerve hammer pizza",
                "cattle citizen wing liar tank chicken clean cause error purse pave despair")

        private val INVALID_MNEMONICS_ENGLISH_15 = listOf(
                "prison riot common gloom claim detect afraid tunnel spare people mean rule pluck use flush",
                "good behind napkin prison maple ghost fold spare club tunnel social fit between fly weasel",
                "waste pause patient member cook lobster peasant snake mountain mixed mushroom soda system steak mask",
                "zone apart enable urge member deputy impose wrong scene talk peasant arctic warrior polar ostrich")

        private val INVALID_MNEMONICS_ENGLISH_18 = listOf(
                "silly remind knee arrow behind admit moon impact grab guide curious fabric junior empty insect wood pool icon",
                "clean pottery produce voyage arrow hello guide panther myself keep dynamic glow total flight right talk alone deposit",
                "visit toast pattern rent buyer journey prefer comfort flush give icon doctor annual power pumpkin shoe pave couch",
                "float lab gadget sea trick toast series chaos universe buyer seat spice cabbage horn glimpse display soup fit")

        private val INVALID_MNEMONICS_ENGLISH_21 = listOf(
                "discover divert waste tape purse travel mystery liquid enjoy stay eager breeze toilet fortune dad click earth hair skate trigger dentist",
                "treat gown begin enjoy hero curious manage survey outside earn genius purse upset bread season dawn action maple more paddle sentence",
                "sail illegal deer fly biology aerobic clarify drift symbol click reopen aisle present aspect shoot home hover moon short vehicle hidden",
                "small tonight umbrella chest replace often deer become short enough match reject miracle vicious interest tip infant breeze depend review interest")

        private val INVALID_MNEMONICS_ENGLISH_24 = listOf(
                "increase interest loyal cactus better evil angle resist useful fashion argue supply around wife trash quote second shift trap regret owner kitten peanut ivory",
                "crack gather slot interest actress solve raw dash increase seven sense castle veteran moon struggle fortune only chair jaguar march arctic rug suffer pyramid",
                "dove horse distance sand blood fire oak target mimic session direct donor assume dog method sadness acid dragon time boost sting regular turkey key",
                "favorite split mammal bubble pluck clutch dragon method stage bullet tornado horse ginger captain chuckle myself dynamic advice wish scale skin drum spray demand")

        private val MNEMONIC_TO_SEED = mapOf(
                "affair pond approve object sheriff stamp build ill electric network veteran honey" to "8779e45171f241fa90d269ebb6e28e72a326594c4472c54a72e01688887a3361a3bce718d1faf5f40fb08c7962d8be7b2aaba6f828953a9ae9b92b5a9e39fd44",
                "thing mansion cart" to "3ad805f87417ddd56568281fe01b4ae4b5def331b0b7096a3f6932751888deee61ed9518358dc924c992c9ca4f39661935859e51e9b30412987cbe27d5e2ffcf",
                "talent digital accuse viable point sniff good wide pioneer rebuild prepare across" to "cb523a00bb5d2fc4531720468bc9907d31b783cfe11876fdef01d994c19d0545f9e8f38b71c6d71688863af6c05a0b74f230679027d740991c09bf99ed565647",
                "bitter expose interest south logic describe awkward seminar involve first season milk" to "fb9ba1c263cd6f1879c46ebb1901a05cc5171311a5a157fb1b9883f7ca1db7165a95839151a7271564d255ee7d48baa6c622a0f9d19b656f82c5e053578d4b65",
                "lava belt sail robot olive hotel scene whisper lend celery shrug envelope" to "5d69ac5dd995387ab6d1135005d5e8cca453e9960271edf808dd50dab04c9d794707eb3e11319e1b07bb3f1ff069630551109e22c8dde96457c0006ffa429afb",
                "net inherit wrong canvas disorder pumpkin noodle average amateur angry dress quote diet better guard" to "178f96c77e164774699c3954b6746ac02ef795e3ae103c79ff6666eb96b9cfc0dab69fbcbe9d261f75a77ebf1dddfa4502e2c8c2a2ffc526f8cb51465e6d3962",
                "gentle mixed always biology original harsh spike swap turkey web regret profit inside random reveal" to "922402feb9b076ca88a12e9980649ee06e5b35f53197d0c978e33ea1fab4e8996bb06bd77de6fb27ccedef95381728740464ee3dec56a7563ae0bd47f4ccfc81",
                "become brand good that damage return welcome unaware sunny weird oil face burden luggage certain" to "881fc818b9cc193abfedac39e2135b829a67ff623b6c736247d11fee36817aa909db45d7029cdfbb2833bc96e3a3baa016c7650e37047f1ea5595803db408a9f",
                "fox humor aware tool letter visa film spin goat flee panther ship toast purpose donkey lobster whisper flower" to "3d6b1db1c1594788caa35924804420002377ab75dc8f7b805ceed8efdfaf450ab47500b516708fa9aa676b9bb7b039824721186fb220b83b81e46d89c09fe989",
                "labor artefact scissors stomach deposit will payment useless world farm flight please edit track rabbit journey stem argue" to "f529766d7259f70a4ce3ccb8a80e6c8e07a387f79d1a8a322b096b6a5c6d5854472247352a9cf880bdb3e33b66060977b4f8517f35f47de80acb68e3a209b3a1",
                "bulb unaware bird disease exile radio midnight robot topic fee action tip hire tell surprise flight sponsor poverty invest crush dose" to "6334e30e8976ef00df1b8d647fba71f043f2cca633cb4a70a78f4a75ea7eeca21959decfa1b350ae4e0891f2b5ce1d56208e38ee7394ed7568c3907dc469554a",
                "hat bar number local earn tooth abstract menu loan enjoy toddler surround casual poem romance axis year dignity baby top seven" to "940b960628895c0ce85c4a7293064b607f695d1343cc2170214512a90d7161f0d8cbd6eeefee32f1f99b46f92622e9096e88c480b9cc6afd7bd418d208eec82d",
                "pair tree silk hen worth degree weird pelican attend almost glimpse lab survey orphan parent junk economy aisle gain wash theory" to "2d44c0a633edd57d43598c6304563917bd56ef35cebb4c21bb59b7f795eb410a11c9240b6690639612fb22c950ef4a604a6dbaa87be5d63ca09ab9353b101d6d",
                "diary essence penalty answer seven wear just casino scatter begin kid repeat peasant trial volume slim student siren raccoon portion depart potato twice digital" to "aa1f65aea38bee747217184271727f00251f5f7bc458275a52052616350a0034333fac098e58892ec1436e37d56448bd763b594af9c7e7f2b93caa11b05c5e74",
                "sense dentist pumpkin cat digital elbow ridge gift payment bring knock upper buyer gold tired spike crowd pupil program river mom swap width candy" to "87d9a7c059a1b09d225980597df3c73d66993bbac4150217722df41535ba850471a0195afc94198bcb3ff2d416857d43b280ca220a9f0015f300b9da655d3b47",
                "umbrella symptom that genre juice universe cloth earth cart crisp system viable tennis zero video rib gift soccer coconut push ivory patrol jeans submit" to "5dc74949166cb76b71777c6cb80d5ef2784a061d1b1aa8b93e73663d188b5b97e03bceffa98ad07e9e32c6b25d166dbe0b9a2f10cfdf9d97bbc42641a5e5cef8")
    }
}
