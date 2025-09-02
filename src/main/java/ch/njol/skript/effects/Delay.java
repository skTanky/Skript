package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.timings.SkriptTimings;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@Name("Delay")
@Description("Delays the script's execution by a given timespan. Please note that delays are not persistent, e.g. trying to create a tempban script with <code>ban player → wait 7 days → unban player</code> will not work if you restart your server anytime within these 7 days. You also have to be careful even when using small delays!")
@Examples({
	"wait 2 minutes",
	"halt for 5 minecraft hours",
	"wait a tick"
})
@Since("1.4")
public class Delay extends Effect {

	static {
		Skript.registerEffect(Delay.class, "(wait|halt) [for] %timespan%");
	}

	@SuppressWarnings("NotNullFieldNotInitialized")
	protected Expression<Timespan> duration;

	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		getParser().setHasDelayBefore(Kleenean.TRUE);

		duration = (Expression<Timespan>) exprs[0];
		if (duration instanceof Literal) { // If we can, do sanity check for delays
			Timespan timespan = ((Literal<Timespan>) duration).getSingle();
			if (timespan.isInfinite()) {
				Skript.error("Delaying for an eternity is not allowed. Use the 'stop' effect instead.");
				return false;
			}
			long millis = timespan.getAs(Timespan.TimePeriod.MILLISECOND);
			if (millis < 50) {
				Skript.warning("Delays less than one tick are not possible, defaulting to one tick.");
			}
		}

		return true;
	}

	@Override
	@Nullable
	protected TriggerItem walk(Event event) {
		debug(event, true);
		long start = Skript.debug() ? System.nanoTime() : 0;
		TriggerItem next = getNext();
		if (next != null && Skript.getInstance().isEnabled()) { // See https://github.com/SkriptLang/Skript/issues/3702
			addDelayedEvent(event);

			Timespan duration = this.duration.getSingle(event);
			if (duration == null)
				return null;
			
			// Back up local variables
			Object localVars = Variables.removeLocals(event);
			
			Bukkit.getScheduler().scheduleSyncDelayedTask(Skript.getInstance(), () -> {
				Skript.debug(getIndentation() + "... continuing after " + (System.nanoTime() - start) / 1_000_000_000. + "s");

				// Re-set local variables
				if (localVars != null)
					Variables.setLocalVariables(event, localVars);

				Object timing = null; // Timings reference must be kept so that it can be stopped after TriggerItem execution
				if (SkriptTimings.enabled()) { // getTrigger call is not free, do it only if we must
					Trigger trigger = getTrigger();
					if (trigger != null)
						timing = SkriptTimings.start(trigger.getDebugLabel());
				}

				TriggerItem.walk(next, event);
				Variables.removeLocals(event); // Clean up local vars, we may be exiting now

				SkriptTimings.stop(timing); // Stop timing if it was even started
			}, Math.max(duration.getAs(Timespan.TimePeriod.TICK), 1)); // Minimum delay is one tick, less than it is useless!
		}
		return null;
	}

	@Override
	protected void execute(Event event) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "wait for " + duration.toString(event, debug) + (event == null ? "" : "...");
	}

	private static final Set<Event> DELAYED =
		Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

	/**
	 * The main method for checking if the execution of {@link TriggerItem}s has been delayed.
	 * @param event The event to check for a delay.
	 * @return Whether {@link TriggerItem} execution has been delayed.
	 */
	public static boolean isDelayed(Event event) {
		return DELAYED.contains(event);
	}

	/**
	 * The main method for marking the execution of {@link TriggerItem}s as delayed.
	 * @param event The event to mark as delayed.
	 */
	public static void addDelayedEvent(Event event) {
		DELAYED.add(event);
	}

}
