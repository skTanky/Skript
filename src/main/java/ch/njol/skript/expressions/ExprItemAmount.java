package ch.njol.skript.expressions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name("Item Amount")
@Description("The amount of an <a href='#itemstack'>item stack</a>.")
@Examples("send \"You have got %item amount of player's tool% %player's tool% in your hand!\" to player")
@Since("2.2-dev24")
public class ExprItemAmount extends SimplePropertyExpression<Object, Long> {

	static {
		register(ExprItemAmount.class, Long.class, "item[[ ]stack] (amount|size|number)", "slots/itemtypes/itemstacks");
	}

	@Override
	public Long convert(final Object item) {
		if (item instanceof ItemType) {
			return (long) ((ItemType) item).getAmount();
		} else if (item instanceof Slot) {
			return (long) ((Slot) item).getAmount();
		} else {
			return (long) ((ItemStack) item).getAmount();
		}
	}

	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		switch (mode) {
			case SET:
			case ADD:
			case RESET:
			case DELETE:
			case REMOVE:
				return CollectionUtils.array(Long.class);
		}
		return null;
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, ChangeMode mode) {
		int amount = delta != null ? ((Number) delta[0]).intValue() : 0;
		switch (mode) {
			case REMOVE:
				amount = -amount;
				// fall through
			case ADD:
				for (Object obj : getExpr().getArray(event))
					if (obj instanceof ItemType) {
						ItemType item = ((ItemType) obj);
						item.setAmount(item.getAmount() + amount);
					} else if (obj instanceof Slot) {
						Slot slot = ((Slot) obj);
						slot.setAmount(slot.getAmount() + amount);
					} else {
						ItemStack item = ((ItemStack) obj);
						item.setAmount(item.getAmount() + amount);
					}
				break;
			case RESET:
			case DELETE:
				amount = 1;
				// fall through
			case SET:
				for (Object obj : getExpr().getArray(event))
					if (obj instanceof ItemType) {
						((ItemType) obj).setAmount(amount);
					} else if (obj instanceof Slot) {
						((Slot) obj).setAmount(amount);
					} else {
						((ItemStack) obj).setAmount(amount);
					}
				break;
		}
	}

	@Override
	public Class<? extends Long> getReturnType() {
		return Long.class;
	}

	@Override
	protected String getPropertyName() {
		return "item amount";
	}
}
