package dev.hogoshi.sioc

import dev.hogoshi.sioc.container.Container
import dev.hogoshi.sioc.handler.AbstractComponentHandler
import dev.hogoshi.sioc.handler.ComponentRegisterHandler
import kotlin.reflect.KClass

class SIoCKt private constructor(private val sioc: Sico) {
    fun start() {
        sioc.start()
    }

    fun stop() {
        sioc.stop()
    }

    fun isRunning(): Boolean = sioc.isRunning

    fun close() {
        sioc.close()
    }

    fun scan(vararg packageNames: String) {
        sioc.scan(*packageNames)
    }

    fun scan(filter: (String) -> Boolean, vararg packageNames: String) {
        sioc.scan({ s -> filter(s) }, *packageNames)
    }

    fun register(clazz: KClass<*>) {
        sioc.register(clazz.java)
    }

    inline fun <reified T : Any> register() {
        register(T::class)
    }

    fun <T : Any> resolve(clazz: KClass<T>): T? {
        return sioc.resolve(clazz.java)
    }

    inline fun <reified T : Any> resolve(): T? {
        return resolve(T::class)
    }

    fun <T : Any> resolve(name: String, clazz: KClass<T>): T? {
        return sioc.resolve(name, clazz.java)
    }

    inline fun <reified T : Any> resolve(name: String): T? {
        return resolve(name, T::class)
    }

    fun addHandler(handler: ComponentRegisterHandler) {
        sioc.addHandler(handler)
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
        sioc.addHandler(wrapperHandler)
    }

    fun removeHandler(handler: ComponentRegisterHandler) {
        sioc.removeHandler(handler)
    }
    
    fun addContainer(name: String, container: Container) {
        sioc.addContainer(name, container)
    }

    companion object {
        @JvmStatic
        fun create(): SIoCKt = SIoCKt(Sico())

        @JvmStatic
        fun getInstance(): SIoCKt = SIoCKt(Sico.getInstance())
    }
}

fun ioc(init: SIoCKt.() -> Unit): SIoCKt {
    val sioc = SIoCKt.create()
    sioc.start()
    sioc.init()
    return sioc
}

inline fun <reified T : Any> SIoCKt.register() {
    register(T::class)
}

inline fun <reified T : Any> SIoCKt.get(): T? = resolve(T::class)

inline fun <reified T : Any> SIoCKt.get(name: String): T? = resolve(name, T::class)

fun SIoCKt.register(clazz: Class<*>) {
    register(clazz.kotlin)
}