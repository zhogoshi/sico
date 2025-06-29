package dev.hogoshi.sico.kotlin

import dev.hogoshi.sico.Sico
import dev.hogoshi.sico.container.Container
import dev.hogoshi.sico.handler.AbstractComponentHandler
import dev.hogoshi.sico.handler.ComponentRegisterHandler
import kotlin.reflect.KClass

class SicoKt private constructor(private val sico: Sico) {
    fun start() {
        sico.start()
    }

    fun stop() {
        sico.stop()
    }

    fun isRunning(): Boolean = sico.isRunning

    fun close() {
        sico.close()
    }

    fun scan(vararg packageNames: String) {
        sico.scan(*packageNames)
    }

    fun scan(filter: (String) -> Boolean, vararg packageNames: String) {
        sico.scan({ s -> filter(s) }, *packageNames)
    }

    fun register(clazz: KClass<*>) {
        sico.register(clazz.java)
    }

    inline fun <reified T : Any> register() {
        register(T::class)
    }

    fun <T : Any> resolve(clazz: KClass<T>): T? {
        return sico.resolve(clazz.java)
    }

    inline fun <reified T : Any> resolve(): T? {
        return resolve(T::class)
    }

    fun <T : Any> resolve(name: String, clazz: KClass<T>): T? {
        return sico.resolve(name, clazz.java)
    }

    inline fun <reified T : Any> resolve(name: String): T? {
        return resolve(name, T::class)
    }

    fun addHandler(handler: ComponentRegisterHandler) {
        sico.addHandler(handler)
    }

    fun addHandler(
        priority: Int = 100,
        phase: ComponentRegisterHandler.Phase = ComponentRegisterHandler.Phase.REGISTRATION,
        vararg supportedAnnotations: KClass<out Annotation>,
        handler: (Class<*>) -> Unit
    ) {
        val javaAnnotations = supportedAnnotations.map { it.java }.toTypedArray()
        
        val wrapperHandler = object : AbstractComponentHandler(priority, phase, *javaAnnotations) {
            override fun handle(componentClass: Class<*>) {
                handler(componentClass)
            }
        }
        sico.addHandler(wrapperHandler)
    }

    fun removeHandler(handler: ComponentRegisterHandler) {
        sico.removeHandler(handler)
    }
    
    fun addContainer(name: String, container: Container) {
        sico.addContainer(name, container)
    }

    companion object {
        @JvmStatic
        fun create(): SicoKt = SicoKt(Sico.getInstance())

        @JvmStatic
        fun getInstance(): SicoKt = SicoKt(Sico.getInstance())
    }
}

fun ioc(init: SicoKt.() -> Unit): SicoKt {
    val sico = SicoKt.create()
    sico.start()
    sico.init()
    return sico
}

inline fun <reified T : Any> SicoKt.register() {
    register(T::class)
}

inline fun <reified T : Any> SicoKt.get(): T? = resolve(T::class)

inline fun <reified T : Any> SicoKt.get(name: String): T? = resolve(name, T::class)

fun SicoKt.register(clazz: Class<*>) {
    register(clazz.kotlin)
}