package com.artillexstudios.axtrade.trade;

import com.artillexstudios.axtrade.AxTrade;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Optional Paper 1.21.7+ native Dialog input adapter.
 *
 * Uses Paper's documented Dialog API reflectively: AxTrade's upstream Spigot
 * 1.20 baseline and its existing sign editor must still load without Paper's
 * Dialog/Adventure 4.22 API classes. No NMS, packets, commands or datapacks.
 *
 * The callback submits exactly the text the player entered. The caller
 * retains all permission, currency validation, trade lifecycle and reopen
 * handling, so the sign and dialog implementations cannot diverge.
 */
public final class PaperCurrencyDialog {
    private static final String PREFIX = "io.papermc.paper.";
    private static final String INPUT_KEY = "amount";
    private static final String CALLBACK = PREFIX + "registry.data.dialog.action.DialogActionCallback";

    private PaperCurrencyDialog() {}

    public static boolean isSupported(Player viewer) {
        try {
            Class<?> dialogLike = Class.forName("net.kyori.adventure.dialog.DialogLike");
            Class.forName(PREFIX + "dialog.Dialog");
            Class.forName(PREFIX + "registry.data.dialog.input.DialogInput");
            Class.forName(CALLBACK);
            // Guard on the actual player class, not an assumed MC version.
            viewer.getClass().getMethod("showDialog", dialogLike);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError error) {
            return false;
        }
    }

    /**
     * @param completion submitted amount, or null when Cancel is clicked.
     * @return true only if the dialog opened; false allows the sign fallback.
     */
    public static boolean open(Player viewer, Component title, Component label,
                               Component confirm, Component cancel, String initial,
                               int maxLength, int width, Consumer<String> completion) {
        if (!isSupported(viewer)) return false;
        try {
            Class<?> dialogClass = Class.forName(PREFIX + "dialog.Dialog");
            Class<?> dialogLike = Class.forName("net.kyori.adventure.dialog.DialogLike");
            Class<?> baseClass = Class.forName(PREFIX + "registry.data.dialog.DialogBase");
            Class<?> baseBuilderClass = Class.forName(PREFIX + "registry.data.dialog.DialogBase$Builder");
            Class<?> inputClass = Class.forName(PREFIX + "registry.data.dialog.input.DialogInput");
            Class<?> inputBuilderClass = Class.forName(PREFIX + "registry.data.dialog.input.TextDialogInput$Builder");
            Class<?> actionClass = Class.forName(PREFIX + "registry.data.dialog.action.DialogAction");
            Class<?> callbackClass = Class.forName(CALLBACK);
            Class<?> optionsClass = Class.forName("net.kyori.adventure.text.event.ClickCallback$Options");
            Class<?> optionsBuilderClass = Class.forName("net.kyori.adventure.text.event.ClickCallback$Options$Builder");
            Class<?> actionButtonClass = Class.forName(PREFIX + "registry.data.dialog.ActionButton");
            Class<?> typeClass = Class.forName(PREFIX + "registry.data.dialog.type.DialogType");
            Class<?> factoryClass = Class.forName(PREFIX + "registry.RegistryBuilderFactory");
            Class<?> entryBuilderClass = Class.forName(PREFIX + "registry.data.dialog.DialogRegistryEntry$Builder");

            Object inputBuilder = inputClass.getMethod("text", String.class, Component.class)
                    .invoke(null, INPUT_KEY, label);
            inputBuilderClass.getMethod("initial", String.class).invoke(inputBuilder, initial);
            inputBuilderClass.getMethod("maxLength", int.class)
                    .invoke(inputBuilder, Math.max(1, Math.min(512, maxLength)));
            inputBuilderClass.getMethod("width", int.class)
                    .invoke(inputBuilder, Math.max(1, Math.min(1024, width)));
            Object input = inputBuilderClass.getMethod("build").invoke(inputBuilder);

            Object baseBuilder = baseClass.getMethod("builder", Component.class).invoke(null, title);
            baseBuilderClass.getMethod("inputs", List.class).invoke(baseBuilder, List.of(input));
            // Esc without a callback would leave the trade in 'input open'
            // state, skipping its normal close/abort handler. Provide Cancel.
            baseBuilderClass.getMethod("canCloseWithEscape", boolean.class).invoke(baseBuilder, false);
            Object base = baseBuilderClass.getMethod("build").invoke(baseBuilder);

            Object optionsBuilder = optionsClass.getMethod("builder").invoke(null);
            Object options = optionsBuilderClass.getMethod("build").invoke(optionsBuilder);
            UUID viewerId = viewer.getUniqueId();
            AtomicBoolean completed = new AtomicBoolean(false);

            Object accepted = action(callbackClass, optionsClass, actionClass, options,
                    callback(callbackClass, viewerId, completed, completion, true));
            Object denied = action(callbackClass, optionsClass, actionClass, options,
                    callback(callbackClass, viewerId, completed, completion, false));
            Object yesButton = actionButtonClass.getMethod("create",
                    Component.class, Component.class, int.class, actionClass)
                    .invoke(null, confirm, null, 120, accepted);
            Object noButton = actionButtonClass.getMethod("create",
                    Component.class, Component.class, int.class, actionClass)
                    .invoke(null, cancel, null, 120, denied);
            Object type = typeClass.getMethod("confirmation", actionButtonClass, actionButtonClass)
                    .invoke(null, yesButton, noButton);

            Object dialog = dialogClass.getMethod("create", Consumer.class).invoke(null,
                    (Consumer<Object>) factory -> {
                        try {
                            Object builder = factoryClass.getMethod("empty").invoke(factory);
                            entryBuilderClass.getMethod("base", baseClass).invoke(builder, base);
                            entryBuilderClass.getMethod("type", typeClass).invoke(builder, type);
                        } catch (ReflectiveOperationException error) {
                            throw new IllegalStateException("Paper Dialog builder failed", error);
                        }
                    });
            viewer.getClass().getMethod("showDialog", dialogLike).invoke(viewer, dialog);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            Throwable cause = error instanceof InvocationTargetException reflection
                    && reflection.getCause() != null ? reflection.getCause() : error;
            AxTrade.getInstance().getLogger().warning("Could not open native currency dialog; "
                    + "falling back to sign input: " + cause);
            return false;
        }
    }

    private static Object action(Class<?> callbackType, Class<?> optionsType,
                                 Class<?> actionType, Object options, Object callback)
            throws ReflectiveOperationException {
        return actionType.getMethod("customClick", callbackType, optionsType)
                .invoke(null, callback, options);
    }

    private static Object callback(Class<?> callbackType, UUID viewerId, AtomicBoolean completed,
                                   Consumer<String> completion, boolean confirm) {
        return Proxy.newProxyInstance(callbackType.getClassLoader(), new Class<?>[]{callbackType},
                (proxy, method, args) -> {
                    if ("accept".equals(method.getName()) && args != null && args.length == 2
                            && args[1] instanceof Player responder
                            && viewerId.equals(responder.getUniqueId())
                            && completed.compareAndSet(false, true)) {
                        String result = null;
                        if (confirm && args[0] != null) {
                            // Invoke the public Paper interface method rather than
                            // CraftBukkit's potentially inaccessible implementation.
                            Object value = Class.forName(PREFIX + "dialog.DialogResponseView")
                                    .getMethod("getText", String.class).invoke(args[0], INPUT_KEY);
                            result = value instanceof String ? (String) value : "";
                        }
                        completion.accept(result);
                    }
                    if ("toString".equals(method.getName())) return "AxTradeCurrencyDialogCallback";
                    if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                    if ("equals".equals(method.getName())) return args != null && args[0] == proxy;
                    return null;
                });
    }
}
