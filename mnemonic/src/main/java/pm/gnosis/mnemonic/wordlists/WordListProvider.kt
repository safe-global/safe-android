package pm.gnosis.mnemonic.wordlists

interface WordListProvider {
    fun all(): Collection<WordList>
    fun get(id: Int): WordList?
}
