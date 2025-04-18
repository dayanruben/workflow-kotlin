package com.squareup.workflow1.ui

import com.squareup.workflow1.ui.ViewRegistry.Entry
import com.squareup.workflow1.ui.ViewRegistry.Key
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * The [ViewEnvironment] service that can be used to display the stream of renderings
 * from a workflow tree as [View] instances. This is the engine behind [AndroidViewRendering],
 * [WorkflowViewStub] and [ViewFactory]. Most apps can ignore [ViewRegistry] as an implementation
 * detail, by using [AndroidViewRendering] to tie their rendering classes to view code.
 *
 * To avoid that coupling between workflow code and the Android runtime, registries can
 * be loaded with [ViewFactory] instances at runtime, and provided as an optional parameter to
 * [WorkflowLayout.start].
 *
 * For example:
 *
 *     val AuthViewFactories = ViewRegistry(
 *       AuthorizingLayoutRunner, LoginLayoutRunner, SecondFactorLayoutRunner
 *     )
 *
 *     val TicTacToeViewFactories = ViewRegistry(
 *       NewGameLayoutRunner, GamePlayLayoutRunner, GameOverLayoutRunner
 *     )
 *
 *     val ApplicationViewFactories = ViewRegistry(ApplicationLayoutRunner) +
 *       AuthViewFactories + TicTacToeViewFactories
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *       super.onCreate(savedInstanceState)
 *
 *       val model: MyViewModel by viewModels()
 *       setContentView(
 *         WorkflowLayout(this).apply { start(model.renderings, ApplicationViewFactories) }
 *       )
 *     }
 *
 *     /** As always, use an androidx ViewModel for state that survives config change. */
 *     class MyViewModel(savedState: SavedStateHandle) : ViewModel() {
 *       val renderings: StateFlow<Any> by lazy {
 *         renderWorkflowIn(
 *           workflow = rootWorkflow,
 *           scope = viewModelScope,
 *           savedStateHandle = savedState
 *         )
 *       }
 *     }
 *
 * In the above example, it is assumed that the `companion object`s of the various
 * decoupled [LayoutRunner] classes honor a convention of implementing [ViewFactory], in
 * aid of this kind of assembly.
 *
 *     class GamePlayLayoutRunner(view: View) : LayoutRunner<GameRendering> {
 *
 *       // ...
 *
 *       companion object : ViewFactory<GameRendering> by LayoutRunner.bind(
 *         R.layout.game_layout, ::GameLayoutRunner
 *       )
 *     }
 */
public interface ViewRegistry {
  /**
   * Identifies a UI factory [Entry] in a [ViewRegistry].
   *
   * @param renderingType the type of view model for which [factoryType] instances can build UI
   * @param factoryType the type of the UI factory that can build UI for [renderingType]
   */
  public class Key<in RenderingT : Any, out FactoryT : Any>(
    public val renderingType: KClass<in RenderingT>,
    public val factoryType: KClass<out FactoryT>
  ) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as Key<*, *>

      if (renderingType != other.renderingType) return false
      return factoryType == other.factoryType
    }

    override fun hashCode(): Int {
      var result = renderingType.hashCode()
      result = 31 * result + factoryType.hashCode()
      return result
    }

    override fun toString(): String {
      return "Key(renderingType=$renderingType, factoryType=$factoryType)"
    }
  }

  /**
   * Implemented by a factory that can build some kind of UI for view models
   * of type [RenderingT], and which can be listed in a [ViewRegistry]. The
   * [Key.factoryType] field of [key] must be the type of this [Entry].
   */
  public interface Entry<in RenderingT : Any> {
    public val key: Key<RenderingT, *>
  }

  /**
   * The set of unique keys which this registry can derive from the renderings passed to
   * [getEntryFor] and for which it knows how to create UI.
   *
   * Used to ensure that duplicate bindings are never registered.
   */
  public val keys: Set<Key<*, *>>

  /**
   * Returns the [Entry] that was registered for the given [key], or null
   * if none was found.
   */
  public fun <RenderingT : Any, FactoryT : Any> getEntryFor(
    key: Key<RenderingT, FactoryT>
  ): Entry<RenderingT>?

  public companion object : ViewEnvironmentKey<ViewRegistry>() {
    override val default: ViewRegistry get() = ViewRegistry()
    override fun combine(
      left: ViewRegistry,
      right: ViewRegistry
    ): ViewRegistry = left.merge(right)
  }
}

public inline fun <RenderingT : Any, reified FactoryT : Any> ViewRegistry.getFactoryFor(
  rendering: RenderingT
): FactoryT? {
  return FactoryT::class.safeCast(getEntryFor(Key(rendering::class, FactoryT::class)))
}

public inline fun <
  reified RenderingT : Any,
  reified FactoryT : Any
  > ViewRegistry.getFactoryFor(): FactoryT? {
  return FactoryT::class.safeCast(getEntryFor(Key(RenderingT::class, FactoryT::class)))
}

public inline operator fun <reified RenderingT : Any, reified FactoryT : Any> ViewRegistry.get(
  key: Key<RenderingT, FactoryT>
): FactoryT? = FactoryT::class.safeCast(getEntryFor(key))

public fun ViewRegistry(vararg bindings: Entry<*>): ViewRegistry =
  TypedViewRegistry(*bindings)

/**
 * Returns a [ViewRegistry] that contains no bindings.
 *
 * Exists as a separate overload from the other two functions to disambiguate between them.
 */
public fun ViewRegistry(): ViewRegistry = TypedViewRegistry()

/**
 * Transforms the receiver to add [entry], throwing [IllegalArgumentException] if the receiver
 * already has a matching [entry]. Use [merge] to replace an existing entry with a new one.
 */
public operator fun ViewRegistry.plus(entry: Entry<*>): ViewRegistry =
  this + ViewRegistry(entry)

/**
 * Transforms the receiver to add all entries from [other].
 *
 * @throws [IllegalArgumentException] if the receiver already has an matching [Entry].
 * Use [merge] to replace existing entries instead.
 */
public operator fun ViewRegistry.plus(other: ViewRegistry): ViewRegistry {
  if (other.keys.isEmpty()) return this
  if (this.keys.isEmpty()) return other
  return CompositeViewRegistry(this, other)
}

/**
 * Returns a new [ViewEnvironment] that adds [registry] to the receiver.
 * If the receiver already has a [ViewRegistry], [ViewEnvironmentKey.combine]
 * is applied as usual to [merge] its entries.
 */
public operator fun ViewEnvironment.plus(registry: ViewRegistry): ViewEnvironment {
  if (this[ViewRegistry] === registry) return this
  if (registry.keys.isEmpty()) return this
  return this + (ViewRegistry to registry)
}

/**
 * Combines the receiver with [other]. If there are conflicting entries,
 * those in [other] are preferred.
 */
public infix fun ViewRegistry.merge(other: ViewRegistry): ViewRegistry {
  if (this === other) return this
  if (other.keys.isEmpty()) return this
  if (this.keys.isEmpty()) return other

  return (keys + other.keys).asSequence()
    .map { other.getEntryFor(it) ?: getEntryFor(it)!! }
    .toList()
    .toTypedArray()
    .let { ViewRegistry(*it) }
}
