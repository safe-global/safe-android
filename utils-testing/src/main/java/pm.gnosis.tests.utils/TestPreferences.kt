package pm.gnosis.tests.utils

import android.content.SharedPreferences

class TestPreferences : SharedPreferences, SharedPreferences.Editor {
    override fun clear(): SharedPreferences.Editor {
        map.clear()
        return this
    }

    override fun putLong(key: String, value: Long): SharedPreferences.Editor {
        map.put(key, value)
        return this
    }

    override fun putInt(key: String, value: Int): SharedPreferences.Editor {
        map.put(key, value)
        return this
    }

    override fun remove(key: String): SharedPreferences.Editor {
        map.remove(key)
        return this
    }

    override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
        map.put(key, value)
        return this
    }

    override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
        map.put(key, value)
        return this
    }

    override fun putString(key: String, value: String): SharedPreferences.Editor {
        map.put(key, value)
        return this
    }

    override fun commit(): Boolean {
        return true
    }

    override fun apply() {
    }

    override fun putStringSet(key: String, value: MutableSet<String>?): SharedPreferences.Editor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val map = HashMap<String, Any>()

    override fun contains(key: String?): Boolean = map.contains(key)

    override fun getBoolean(key: String?, value: Boolean): Boolean = (map[key] as? Boolean) ?: value

    override fun getInt(key: String?, value: Int) =(map[key] as? Int) ?: value

    override fun getAll(): MutableMap<String, *> = map

    override fun edit(): SharedPreferences.Editor = this

    override fun getLong(key: String?, value: Long) = (map[key] as? Long) ?: value

    override fun getFloat(key: String?, value: Float) = (map[key] as? Float) ?: value

    override fun getString(key: String?, value: String?) = (map[key] as? String) ?: value

    override fun getStringSet(key: String?, value: MutableSet<String>?): MutableSet<String> {
        TODO("not implemented")
    }

    override fun unregisterOnSharedPreferenceChangeListener(key: SharedPreferences.OnSharedPreferenceChangeListener?) {
        TODO("not implemented")
    }

    override fun registerOnSharedPreferenceChangeListener(key: SharedPreferences.OnSharedPreferenceChangeListener?) {
        TODO("not implemented")
    }

}
