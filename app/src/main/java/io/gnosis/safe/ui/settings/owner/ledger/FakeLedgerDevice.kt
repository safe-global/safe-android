package io.gnosis.safe.ui.settings.owner.ledger

import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress

class FakeLedgerDevice : LedgerAddressProvider {
    private var derivationPath = ""
    private var addresses = listOf(
        "0xE86935943315293154c7AD63296b4e1adAc76364".asEthereumAddress()!!,
        "0x5c9E7b93900536D9cc5559b881375Bae93c933D0".asEthereumAddress()!!,
        "0xD28293bf13549Abb49Ed1D83D515301A05E3Fc8d".asEthereumAddress()!!,
        "0x47fc70650f612E9c1D149f7508b593f344beb202".asEthereumAddress()!!,
        "0x41fEE475Ac1AeE93cCfF2E0D3625daAAff366d4d".asEthereumAddress()!!,
        "0x6182de12A6D5d137Deceb72BB4aAF9669508160c".asEthereumAddress()!!,
        "0x611251d2CfE060DCF375443eD578Ec1863a59a9e".asEthereumAddress()!!,
        "0x61fE050055b374cf6CAd4E3A7E0A967c64A1557d".asEthereumAddress()!!,
        "0x879165bB7dbDAa22Ea0deF73EBA4D642bAA556d9".asEthereumAddress()!!,
        "0xF36Cea6A0368bF038F20b320b563A82CA5f08431".asEthereumAddress()!!,
        "0xB61eB70cEfcbB3D998bed111d911cdb003B3BCfF".asEthereumAddress()!!,
        "0x411B055bD0ECeEDEb0767B09F9894949c4180424".asEthereumAddress()!!,
        "0x0fA3158fa6aa4062AdA77D54b6da37C52Ed58043".asEthereumAddress()!!,
        "0x6601C055b70669f12f4CB92e7be8C3e256eD307E".asEthereumAddress()!!,
        "0x01Ce6D54935c522fD1023f79Bb34dC113c06C700".asEthereumAddress()!!,
        "0x4023655e9d57575206Eef13986064eA28a10834d".asEthereumAddress()!!,
        "0x53477340127e72644C8E0BeBff7bE4aA88b3D176".asEthereumAddress()!!,
        "0xd3df07ab816262C5A4b1a878b6F0690c7Fe71DD6".asEthereumAddress()!!,
        "0x3eBA4E8f1511FD9663D520c77e0B3AaD74890513".asEthereumAddress()!!,
        "0x54242A8Ca0DAcD549147DB7282C7056297930777".asEthereumAddress()!!,
        "0x883f548608cc56cC16FBa74c4Edf88786BeE42a9".asEthereumAddress()!!,
        "0xA384B177310FFEF4290a69B9E2a96d65Ff01453E".asEthereumAddress()!!,
        "0x55572fa9c27E098bD4C817b3b746316672774735".asEthereumAddress()!!,
        "0x71c9941CC19A3F4Fd348d8722b69cf1310623d8C".asEthereumAddress()!!,
        "0x0199DF34D369ab74264A00B6E549d3ea1fe4D876".asEthereumAddress()!!,
        "0xADaA79ABc96610C34d5288A2ff0C051F1090942D".asEthereumAddress()!!,
        "0x62b794b83f2A11e770c320EB4f858e9A330DA24C".asEthereumAddress()!!,
        "0xA60373D79d9ef360cE0D77A4fD88c0E6826E3E3d".asEthereumAddress()!!,
        "0x6601C055b70669f12f4CB92e7be8C3e256eD307E".asEthereumAddress()!!,
        "0x01Ce6D54935c522fD1023f79Bb34dC113c06C700".asEthereumAddress()!!,
        "0x4023655e9d57575206Eef13986064eA28a10834d".asEthereumAddress()!!,
        "0x53477340127e72644C8E0BeBff7bE4aA88b3D176".asEthereumAddress()!!,
        "0xd3df07ab816262C5A4b1a878b6F0690c7Fe71DD6".asEthereumAddress()!!,
        "0x3eBA4E8f1511FD9663D520c77e0B3AaD74890513".asEthereumAddress()!!,
        "0x54242A8Ca0DAcD549147DB7282C7056297930777".asEthereumAddress()!!,
        "0x883f548608cc56cC16FBa74c4Edf88786BeE42a9".asEthereumAddress()!!,
        "0xA384B177310FFEF4290a69B9E2a96d65Ff01453E".asEthereumAddress()!!,
        "0x55572fa9c27E098bD4C817b3b746316672774735".asEthereumAddress()!!,
        "0x71c9941CC19A3F4Fd348d8722b69cf1310623d8C".asEthereumAddress()!!,
        "0x0199DF34D369ab74264A00B6E549d3ea1fe4D876".asEthereumAddress()!!,
        "0xADaA79ABc96610C34d5288A2ff0C051F1090942D".asEthereumAddress()!!,
        "0x62b794b83f2A11e770c320EB4f858e9A330DA24C".asEthereumAddress()!!,
        "0xA60373D79d9ef360cE0D77A4fD88c0E6826E3E3d".asEthereumAddress()!!,
        "0x6601C055b70669f12f4CB92e7be8C3e256eD307E".asEthereumAddress()!!,
        "0x01Ce6D54935c522fD1023f79Bb34dC113c06C700".asEthereumAddress()!!,
        "0x4023655e9d57575206Eef13986064eA28a10834d".asEthereumAddress()!!,
        "0x53477340127e72644C8E0BeBff7bE4aA88b3D176".asEthereumAddress()!!,
        "0xd3df07ab816262C5A4b1a878b6F0690c7Fe71DD6".asEthereumAddress()!!,
        "0x3eBA4E8f1511FD9663D520c77e0B3AaD74890513".asEthereumAddress()!!,
        "0x54242A8Ca0DAcD549147DB7282C7056297930777".asEthereumAddress()!!,
        "0x883f548608cc56cC16FBa74c4Edf88786BeE42a9".asEthereumAddress()!!,
        "0xA384B177310FFEF4290a69B9E2a96d65Ff01453E".asEthereumAddress()!!,
        "0x55572fa9c27E098bD4C817b3b746316672774735".asEthereumAddress()!!,
        "0x71c9941CC19A3F4Fd348d8722b69cf1310623d8C".asEthereumAddress()!!,
        "0x0199DF34D369ab74264A00B6E549d3ea1fe4D876".asEthereumAddress()!!,
        "0xADaA79ABc96610C34d5288A2ff0C051F1090942D".asEthereumAddress()!!,
        "0x62b794b83f2A11e770c320EB4f858e9A330DA24C".asEthereumAddress()!!,
        "0xA60373D79d9ef360cE0D77A4fD88c0E6826E3E3d".asEthereumAddress()!!,
        "0x6601C055b70669f12f4CB92e7be8C3e256eD307E".asEthereumAddress()!!,
        "0x01Ce6D54935c522fD1023f79Bb34dC113c06C700".asEthereumAddress()!!,
        "0x4023655e9d57575206Eef13986064eA28a10834d".asEthereumAddress()!!,
        "0x53477340127e72644C8E0BeBff7bE4aA88b3D176".asEthereumAddress()!!,
        "0xd3df07ab816262C5A4b1a878b6F0690c7Fe71DD6".asEthereumAddress()!!,
        "0x3eBA4E8f1511FD9663D520c77e0B3AaD74890513".asEthereumAddress()!!,
        "0x54242A8Ca0DAcD549147DB7282C7056297930777".asEthereumAddress()!!,
        "0x883f548608cc56cC16FBa74c4Edf88786BeE42a9".asEthereumAddress()!!,
        "0xA384B177310FFEF4290a69B9E2a96d65Ff01453E".asEthereumAddress()!!,
        "0x55572fa9c27E098bD4C817b3b746316672774735".asEthereumAddress()!!,
        "0x71c9941CC19A3F4Fd348d8722b69cf1310623d8C".asEthereumAddress()!!,
        "0x0199DF34D369ab74264A00B6E549d3ea1fe4D876".asEthereumAddress()!!,
        "0xADaA79ABc96610C34d5288A2ff0C051F1090942D".asEthereumAddress()!!,
        "0x62b794b83f2A11e770c320EB4f858e9A330DA24C".asEthereumAddress()!!,
        "0xA60373D79d9ef360cE0D77A4fD88c0E6826E3E3d".asEthereumAddress()!!,
        "0xcEAEAf0b8FFB27860eddA2843B3b601956595dd4".asEthereumAddress()!!
    )

    override fun initialize(derivationPath: String) {
        if (derivationPath.isBlank()) {
            addresses = addresses.reversed()
        }
    }

    override fun addressesForRange(range: LongRange): List<Solidity.Address> {
        return addresses.subList(range.first.toInt(), range.last.toInt())
    }

    override fun addressesForPage(start: Long, pageSize: Int): List<Solidity.Address> {
        return addresses.subList(start.toInt(), (start + pageSize).toInt())
    }
}

interface LedgerAddressProvider {
    fun initialize(derivationPath: String)
    fun addressesForRange(range: LongRange): List<Solidity.Address>
    fun addressesForPage(start: Long, pageSize: Int): List<Solidity.Address>
}
