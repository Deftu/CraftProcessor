package xyz.deftu.craftprocessor

class ArgumentMap {
    companion object {
        fun parse(input: Array<String>): ArgumentMap {
            val arguments = ArgumentMap()
            val inputList = input.toList()
            for (item in inputList) {
                val index = inputList.indexOf(item)
                if (input.size - 1 < index || !item.startsWith("--")) continue

                var value = if (index + 1 >= input.size) "" else input[index + 1]
                if (value.startsWith("--")) value = ""

                val name = item.substring(2)
                arguments[name] = value
            }

            return arguments
        }
    }

    private val internalMap = mutableMapOf<String, List<String>>()

    fun put(key: String, value: String) {
        val list = internalMap[key] ?: emptyList()
        internalMap[key] = list + value
    }

    fun putIfAbsent(key: String, value: String) {
        if (key in internalMap) return
        put(key, value)
    }

    operator fun set(key: String, value: String) = put(key, value)
    fun getAll(key: String) = internalMap[key]
    fun getSingular(key: String) = getAll(key)?.firstOrNull()
    operator fun get(key: String) = getSingular(key)
    fun has(key: String) = key in internalMap
    fun remove(key: String) = internalMap.remove(key)
    fun removeAndGet(key: String) = internalMap.remove(key)?.firstOrNull()

    fun toMap() = internalMap.toMap()
    fun toList() = internalMap.toList()
}
