package pm.gnosis.crypto

/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//Changed the original file (Kotlin conversion)
import java.io.*
import java.security.Provider
import java.security.SecureRandomSpi
import java.security.Security

/**
 * A SecureRandom implementation that is able to override the standard JVM provided implementation, and which simply
 * serves random numbers by reading /dev/urandom. That is, it delegates to the kernel on UNIX systems and is unusable on
 * other platforms. Attempts to manually set the seed are ignored. There is no difference between seed bytes and
 * non-seed bytes, they are all from the same source.
 */
class LinuxSecureRandom : SecureRandomSpi() {
    companion object {
        const val PROVIDER_NAME = "LinuxSecureRandom"
        const val PATH = "/dev/urandom"
        private val urandom: FileInputStream

        private class LinuxSecureRandomProvider : Provider(PROVIDER_NAME, 1.0, "A Linux specific random number provider that uses /dev/urandom") {
            init {
                put("SecureRandom.LinuxSecureRandom", LinuxSecureRandom::class.java.name)
            }
        }

        init {
            try {
                val file = File(PATH)
                // This stream is deliberately leaked.
                urandom = FileInputStream(file)
                if (urandom.read() == -1)
                    throw RuntimeException("$PATH not readable?")
                // Now override the default SecureRandom implementation with this one.
                val position = Security.insertProviderAt(LinuxSecureRandomProvider(), 1)

                System.out.println(
                    if (position != -1)
                        "Secure randomness will be read from $file only." else
                        "Randomness is already secure."
                )
            } catch (e: FileNotFoundException) {
                // Should never happen.
                System.err.println("/dev/urandom does not appear to exist or is not openable")
                throw RuntimeException(e)
            } catch (e: IOException) {
                System.err.println("/dev/urandom does not appear to be readable")
                throw RuntimeException(e)
            }
        }
    }

    private val dis: DataInputStream

    init {
        // DataInputStream is not thread safe, so each random object has its own.
        dis = DataInputStream(urandom)
    }

    override fun engineSetSeed(bytes: ByteArray) {
        // Ignore.
    }

    override fun engineNextBytes(bytes: ByteArray) {
        try {
            dis.readFully(bytes) // This will block until all the bytes can be read.
        } catch (e: IOException) {
            throw RuntimeException(e) // Fatal error. Do not attempt to recover from this.
        }
    }

    override fun engineGenerateSeed(i: Int): ByteArray {
        val bits = ByteArray(i)
        engineNextBytes(bits)
        return bits
    }
}
