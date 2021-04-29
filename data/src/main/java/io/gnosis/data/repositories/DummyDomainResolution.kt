package io.gnosis.data.repositories

import com.unstoppabledomains.config.network.model.Network
import com.unstoppabledomains.resolution.DomainResolution
import com.unstoppabledomains.resolution.TickerVersion
import com.unstoppabledomains.resolution.dns.DnsRecord
import com.unstoppabledomains.resolution.dns.DnsRecordsType
import com.unstoppabledomains.resolution.naming.service.NamingServiceType
import io.gnosis.data.utils.ExcludeClassFromJacocoGeneratedReport

@ExcludeClassFromJacocoGeneratedReport
class DummyDomainResolution : DomainResolution {
    override fun getOwner(domain: String?): String {
        TODO("Not yet implemented")
    }

    override fun email(domain: String?): String {
        TODO("Not yet implemented")
    }

    override fun getEmail(domain: String?): String {
        TODO("Not yet implemented")
    }

    override fun namehash(domain: String?): String {
        TODO("Not yet implemented")
    }

    override fun owner(domain: String?): String {
        TODO("Not yet implemented")
    }

    override fun addr(domain: String?, ticker: String?): String {
        TODO("Not yet implemented")
    }

    override fun getNetwork(type: NamingServiceType?): Network {
        TODO("Not yet implemented")
    }

    override fun ipfsHash(domain: String?): String {
        TODO("Not yet implemented")
    }

    override fun getRecord(domain: String?, recordKey: String?): String {
        TODO("Not yet implemented")
    }

    override fun getMultiChainAddress(domain: String?, ticker: String?, chain: String?): String {
        TODO("Not yet implemented")
    }

    override fun isSupported(domain: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun getDns(domain: String?, types: MutableList<DnsRecordsType>?): MutableList<DnsRecord> {
        TODO("Not yet implemented")
    }

    override fun getAddress(domain: String?, ticker: String?): String {
        TODO("Not yet implemented")
    }

    override fun getIpfsHash(domain: String?): String {
        TODO("Not yet implemented")
    }

    override fun getUsdt(domain: String?, version: TickerVersion?): String {
        TODO("Not yet implemented")
    }

    override fun getNamehash(domain: String?): String {
        TODO("Not yet implemented")
    }
}
