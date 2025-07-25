package ch.njol.skript.lang;

import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.lang.structure.StructureInfo;

import ch.njol.skript.SkriptAPIException;
import org.skriptlang.skript.registration.SyntaxOrigin;
import org.skriptlang.skript.util.Priority;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @param <E> the syntax element this info is for
 */
public class SyntaxElementInfo<E extends SyntaxElement> implements SyntaxInfo<E> {

	// todo: 2.9 make all fields private
	public final Class<E> elementClass;
	public final String[] patterns;
	public final String originClassPath;

	public SyntaxElementInfo(String[] patterns, Class<E> elementClass, String originClassPath) throws IllegalArgumentException {
		if (Modifier.isAbstract(elementClass.getModifiers()))
			throw new SkriptAPIException("Class " + elementClass.getName() + " is abstract");

		this.patterns = patterns;
		this.elementClass = elementClass;
		this.originClassPath = originClassPath;
		try {
			elementClass.getConstructor();
		} catch (final NoSuchMethodException e) {
			// throwing an Exception throws an (empty) ExceptionInInitializerError instead, thus an Error is used
			throw new Error(elementClass + " does not have a public nullary constructor", e);
		} catch (final SecurityException e) {
			throw new IllegalStateException("Skript cannot run properly because a security manager is blocking it!");
		}
	}

	/**
	 * Get the class that represents this element.
	 * @return The Class of the element
	 */
	public Class<E> getElementClass() {
		return elementClass;
	}

	/**
	 * Get the patterns of this syntax element.
	 * @return Array of Skript patterns for this element
	 */
	public String[] getPatterns() {
		return Arrays.copyOf(patterns, patterns.length);
	}

	/**
	 * Get the original classpath for this element.
	 * @return The original ClassPath for this element
	 */
	public String getOriginClassPath() {
		return originClassPath;
	}

	@Contract("_ -> new")
	@ApiStatus.Internal
	@ApiStatus.Experimental
	@SuppressWarnings("unchecked")
	public static <I extends SyntaxElementInfo<E>, E extends SyntaxElement> I fromModern(SyntaxInfo<? extends E> info) {
		if (info instanceof SyntaxElementInfo<? extends E> oldInfo) {
			return (I) oldInfo;
		} else if (info instanceof BukkitSyntaxInfos.Event<?> event) {
			// We must first go back to the raw input
			String rawName = event.name().startsWith("On ")
					? event.name().substring(3)
					: "*" + event.name();
			SkriptEventInfo<?> eventInfo = new SkriptEventInfo<>(
					rawName, event.patterns().toArray(new String[0]),
					event.type(), event.origin().name(),
					(Class<? extends Event>[]) event.events().toArray(new Class<?>[0]));
			String documentationId = event.documentationId();
			if (documentationId != null)
				eventInfo.documentationID(documentationId);
			eventInfo.listeningBehavior(event.listeningBehavior())
					.since(event.since().toArray(new String[0]))
					.description(event.description().toArray(new String[0]))
					.examples(event.examples().toArray(new String[0]))
					.keywords(event.keywords().toArray(new String[0]))
					.requiredPlugins(event.requiredPlugins().toArray(new String[0]));

			return (I) eventInfo;
		} else if (info instanceof SyntaxInfo.Structure<?> structure) {
			return (I) new StructureInfo<>(structure.patterns().toArray(new String[0]), structure.type(),
					structure.origin().name(), structure.entryValidator(), structure.nodeType());
		} else if (info instanceof SyntaxInfo.Expression<?, ?> expression) {
			return (I) fromModernExpression(expression);
		}

		return (I) new SyntaxElementInfo<>(info.patterns().toArray(new String[0]), info.type(), info.origin().name());
	}
	
	@Contract("_ -> new")
	@ApiStatus.Experimental
	private static <E extends ch.njol.skript.lang.Expression<R>, R> ExpressionInfo<E, R> fromModernExpression(SyntaxInfo.Expression<E, R> info) {
		return new ExpressionInfo<>(
				info.patterns().toArray(new String[0]), info.returnType(),
				info.type(), info.origin().name(), ExpressionType.fromModern(info.priority())
		);
	}

	// Registration API Compatibility

	@Override
	@ApiStatus.Internal
	public Builder<? extends Builder<?, E>, E> toBuilder() {
		// should not be called for this object
		throw new UnsupportedOperationException();
	}

	@Override
	@ApiStatus.Internal
	public SyntaxOrigin origin() {
		return () -> originClassPath;
	}

	@Override
	@ApiStatus.Internal
	public Class<E> type() {
		return getElementClass();
	}

	@Override
	@ApiStatus.Internal
	public E instance() {
		try {
			return type().getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException |
				 NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	@ApiStatus.Internal
	public @Unmodifiable Collection<String> patterns() {
		return List.of(getPatterns());
	}

	@Override
	@ApiStatus.Internal
	public Priority priority() {
		return SyntaxInfo.COMBINED;
	}

}
