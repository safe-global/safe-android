package pm.gnosis.heimdall.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pm.gnosis.heimdall.data.db.daos.*
import pm.gnosis.heimdall.data.db.models.*
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.utils.asEthereumAddressString

@Database(
    entities = [
        AddressBookEntryDb::class,
        AuthenticatorInfoDb::class,
        ERC20TokenDb::class,
        GnosisSafeInfoDb::class,
        GnosisSafeDb::class,
        PendingGnosisSafeDb::class,
        RecoveringGnosisSafeDb::class,
        TransactionDescriptionDb::class,
        TransactionPublishStatusDb::class
    ], version = 4
)
@TypeConverters(BigIntegerConverter::class, SolidityAddressConverter::class, WeiConverter::class, EncryptedByteArray.Converter::class)
abstract class ApplicationDb : RoomDatabase() {
    companion object {
        const val DB_NAME = "gnosis-safe-db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `${GnosisSafeInfoDb.TABLE_NAME}`
                            (`${GnosisSafeInfoDb.COL_SAFE_ADDRESS}` TEXT NOT NULL,
                            `${GnosisSafeInfoDb.COL_OWNER_ADDRESS}` TEXT NOT NULL,
                            `${GnosisSafeInfoDb.COL_OWNER_PRIVATE_KEY}` TEXT NOT NULL,
                            PRIMARY KEY(`${GnosisSafeInfoDb.COL_SAFE_ADDRESS}`))"""
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """ALTER TABLE `${GnosisSafeInfoDb.TABLE_NAME}`
                            ADD COLUMN `${GnosisSafeInfoDb.COL_PAYMENT_TOKEN_ADDRESS}` TEXT NOT NULL
                            DEFAULT '${ERC20Token.ETHER_TOKEN.address.asEthereumAddressString()}'
                            """
                )
                database.execSQL(
                    """ALTER TABLE `${GnosisSafeInfoDb.TABLE_NAME}`
                            ADD COLUMN `${GnosisSafeInfoDb.COL_PAYMENT_TOKEN_SYMBOL}` TEXT NOT NULL
                            DEFAULT '${ERC20Token.ETHER_TOKEN.symbol}'
                            """
                )
                database.execSQL(
                    """ALTER TABLE `${GnosisSafeInfoDb.TABLE_NAME}`
                            ADD COLUMN `${GnosisSafeInfoDb.COL_PAYMENT_TOKEN_NAME}` TEXT NOT NULL
                            DEFAULT '${ERC20Token.ETHER_TOKEN.name}'
                            """
                )
                database.execSQL(
                    """ALTER TABLE `${GnosisSafeInfoDb.TABLE_NAME}`
                            ADD COLUMN `${GnosisSafeInfoDb.COL_PAYMENT_TOKEN_DECIMALS}` INTEGER NOT NULL
                            DEFAULT ${ERC20Token.ETHER_TOKEN.decimals}
                            """
                )
                database.execSQL(
                    """ALTER TABLE `${GnosisSafeInfoDb.TABLE_NAME}`
                            ADD COLUMN `${GnosisSafeInfoDb.COL_PAYMENT_TOKEN_ICON}` TEXT
                            """
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `${AuthenticatorInfoDb.TABLE_NAME}`
                            (`${AuthenticatorInfoDb.COL_ADDRESS}` TEXT NOT NULL,
                            `${AuthenticatorInfoDb.COL_TYPE}` INTEGER NOT NULL,
                            `${AuthenticatorInfoDb.COL_KEY_INDEX}` INTEGER,
                            PRIMARY KEY(`${AuthenticatorInfoDb.COL_ADDRESS}`))"""
                )
            }
        }
    }

    abstract fun addressBookDao(): AddressBookDao
    abstract fun authenticatorInfoDao(): AuthenticatorInfoDao
    abstract fun descriptionsDao(): DescriptionsDao
    abstract fun erc20TokenDao(): ERC20TokenDao
    abstract fun gnosisSafeDao(): GnosisSafeDao
}
