package ch.njol.skript;

import ch.njol.skript.aliases.Aliases;
import ch.njol.skript.bukkitutil.BurgerHelper;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.data.BukkitClasses;
import ch.njol.skript.classes.data.BukkitEventValues;
import ch.njol.skript.classes.data.DefaultComparators;
import ch.njol.skript.classes.data.DefaultConverters;
import ch.njol.skript.classes.data.DefaultFunctions;
import ch.njol.skript.classes.data.DefaultOperations;
import ch.njol.skript.classes.data.JavaClasses;
import ch.njol.skript.classes.data.SkriptClasses;
import ch.njol.skript.command.Commands;
import ch.njol.skript.doc.Documentation;
import ch.njol.skript.events.EvtSkript;
import ch.njol.skript.hooks.Hook;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.Condition.ConditionType;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Message;
import ch.njol.skript.localization.PluralizingArgsMessage;
import ch.njol.skript.log.BukkitLoggerFilter;
import ch.njol.skript.log.CountingLogHandler;
import ch.njol.skript.log.ErrorDescLogHandler;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.LogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.log.TestingLogHandler;
import ch.njol.skript.log.Verbosity;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.registrations.Feature;
import ch.njol.skript.test.runner.EffObjectives;
import ch.njol.skript.test.runner.SkriptAsyncJUnitTest;
import ch.njol.skript.test.runner.SkriptJUnitTest;
import ch.njol.skript.test.runner.SkriptTestEvent;
import ch.njol.skript.test.runner.TestMode;
import ch.njol.skript.test.runner.TestTracker;
import ch.njol.skript.timings.SkriptTimings;
import ch.njol.skript.update.ReleaseManifest;
import ch.njol.skript.update.ReleaseStatus;
import ch.njol.skript.update.UpdateManifest;
import ch.njol.skript.util.Date;
import ch.njol.skript.util.EmptyStacktraceException;
import ch.njol.skript.util.ExceptionUtils;
import ch.njol.skript.util.FileUtils;
import ch.njol.skript.util.Task;
import ch.njol.skript.util.Utils;
import ch.njol.skript.util.Version;
import ch.njol.skript.util.chat.BungeeConverter;
import ch.njol.skript.util.chat.ChatMessages;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Closeable;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.iterator.CheckedIterator;
import ch.njol.util.coll.iterator.EnumerationIterable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.papermc.lib.PaperLib;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.After;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.skriptlang.skript.bukkit.SkriptMetrics;
import org.skriptlang.skript.bukkit.breeding.BreedingModule;
import org.skriptlang.skript.bukkit.damagesource.DamageSourceModule;
import org.skriptlang.skript.bukkit.displays.DisplayModule;
import org.skriptlang.skript.bukkit.fishing.FishingModule;
import org.skriptlang.skript.bukkit.furnace.FurnaceModule;
import org.skriptlang.skript.bukkit.input.InputModule;
import org.skriptlang.skript.bukkit.log.runtime.BukkitRuntimeErrorConsumer;
import org.skriptlang.skript.bukkit.loottables.LootTableModule;
import org.skriptlang.skript.bukkit.registration.BukkitRegistryKeys;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.bukkit.tags.TagModule;
import org.skriptlang.skript.lang.comparator.Comparator;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.experiment.ExperimentRegistry;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.lang.structure.StructureInfo;
import org.skriptlang.skript.log.runtime.RuntimeErrorManager;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxOrigin;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.ClassLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

// TODO meaningful error if someone uses an %expression with percent signs% outside of text or a variable

/**
 * <b>Skript</b> - A Bukkit plugin to modify how Minecraft behaves without having to write a single line of code (You'll likely be writing some code though if you're reading this
 * =P)
 * <p>
 * Use this class to extend this plugin's functionality by adding more {@link Condition conditions}, {@link Effect effects}, {@link SimpleExpression expressions}, etc.
 * <p>
 * If your plugin.yml contains <tt>'depend: [Skript]'</tt> then your plugin will not start at all if Skript is not present. Add <tt>'softdepend: [Skript]'</tt> to your plugin.yml
 * if you want your plugin to work even if Skript isn't present, but want to make sure that Skript gets loaded before your plugin.
 * <p>
 * If you use 'softdepend' you can test whether Skript is loaded with <tt>'Bukkit.getPluginManager().getPlugin(&quot;Skript&quot;) != null'</tt>
 * <p>
 * Once you made sure that Skript is loaded you can use <code>Skript.getInstance()</code> whenever you need a reference to the plugin, but you likely won't need it since all API
 * methods are static.
 *
 * @author Peter Güttinger
 * @see #registerAddon(JavaPlugin)
 * @see #registerCondition(Class, String...)
 * @see #registerEffect(Class, String...)
 * @see #registerExpression(Class, Class, ExpressionType, String...)
 * @see #registerEvent(String, Class, Class, String...)
 * @see EventValues#registerEventValue(Class, Class, Converter, int)
 * @see Classes#registerClass(ClassInfo)
 * @see Comparators#registerComparator(Class, Class, Comparator)
 * @see Converters#registerConverter(Class, Class, Converter)
 */
public final class Skript extends JavaPlugin implements Listener {

	// ================ PLUGIN ================

	@Nullable
	private static Skript instance = null;

	private static org.skriptlang.skript.@UnknownNullability Skript skript = null;
	private static org.skriptlang.skript.@UnknownNullability Skript unmodifiableSkript = null;

	private static boolean disabled = false;
	private static boolean partDisabled = false;

	public static Skript getInstance() {
		if (instance == null)
			throw new IllegalStateException();
		return instance;
	}

	@ApiStatus.Experimental
	public static org.skriptlang.skript.Skript instance() {
		if (unmodifiableSkript == null) {
			throw new SkriptAPIException("Skript is still initializing");
		}
		return unmodifiableSkript;
	}

	/**
	 * Current updater instance used by Skript.
	 */
	@Nullable
	private SkriptUpdater updater;

	public Skript() throws IllegalStateException {
		if (instance != null)
			throw new IllegalStateException("Cannot create multiple instances of Skript!");
		instance = this;
	}

	private static Version minecraftVersion = new Version(666), UNKNOWN_VERSION = new Version(666);
	private static ServerPlatform serverPlatform = ServerPlatform.BUKKIT_UNKNOWN; // Start with unknown... onLoad changes this

	/**
	 * Check minecraft version and assign it to minecraftVersion field
	 * This method is created to update MC version before onEnable method
	 */
	public static void updateMinecraftVersion() {
		String bukkitV = Bukkit.getBukkitVersion();
		Matcher m = Pattern.compile("\\d+\\.\\d+(\\.\\d+)?").matcher(bukkitV);
		if (!m.find()) {
			minecraftVersion = new Version(666, 0, 0);
		} else {
			minecraftVersion = new Version("" + m.group());
		}
	}

	@Nullable
	private static Version version = null;
	@Deprecated(since = "2.9.0", forRemoval = true) // TODO this field will be replaced by a proper registry later
	private static @UnknownNullability ExperimentRegistry experimentRegistry;

	public static Version getVersion() {
		final Version v = version;
		if (v == null)
			throw new IllegalStateException();
		return v;
	}

	public static final Message
		m_invalid_reload = new Message("skript.invalid reload"),
		m_finished_loading = new Message("skript.finished loading"),
		m_no_errors = new Message("skript.no errors"),
		m_no_scripts = new Message("skript.no scripts");
	private static final PluralizingArgsMessage m_scripts_loaded = new PluralizingArgsMessage("skript.scripts loaded");

	private static final Message WARNING_MESSAGE = new Message("skript.warning message");
	private static final Message RESTART_MESSAGE = new Message("skript.restart message");

	public static String getWarningMessage() {
		return WARNING_MESSAGE.getValueOrDefault("It appears that /reload or another plugin reloaded Skript. This is not supported behaviour and may cause issues.");
	}

	public static String getRestartMessage() {
		return RESTART_MESSAGE.getValueOrDefault("Please consider restarting the server instead.");
	}

	public static ServerPlatform getServerPlatform() {
		if (classExists("net.glowstone.GlowServer")) {
			return ServerPlatform.BUKKIT_GLOWSTONE; // Glowstone has timings too, so must check for it first
		} else if (classExists("co.aikar.timings.Timings")) {
			return ServerPlatform.BUKKIT_PAPER; // Could be Sponge, but it doesn't work at all at the moment
		} else if (classExists("org.spigotmc.SpigotConfig")) {
			return ServerPlatform.BUKKIT_SPIGOT;
		} else if (classExists("org.bukkit.craftbukkit.CraftServer") || classExists("org.bukkit.craftbukkit.Main")) {
			// At some point, CraftServer got removed or moved
			return ServerPlatform.BUKKIT_CRAFTBUKKIT;
		} else { // Probably some ancient Bukkit implementation
			return ServerPlatform.BUKKIT_UNKNOWN;
		}
	}

	/**
	 * Checks if server software and Minecraft version are supported.
	 * Prints errors or warnings to console if something is wrong.
	 * @return Whether Skript can continue loading at all.
	 */
	private static boolean checkServerPlatform() {
		String bukkitV = Bukkit.getBukkitVersion();
		Matcher m = Pattern.compile("\\d+\\.\\d+(\\.\\d+)?").matcher(bukkitV);
		if (!m.find()) {
			Skript.error("The Bukkit version '" + bukkitV + "' does not contain a version number which is required for Skript to enable or disable certain features. " +
					"Skript will still work, but you might get random errors if you use features that are not available in your version of Bukkit.");
			minecraftVersion = new Version(666, 0, 0);
		} else {
			minecraftVersion = new Version("" + m.group());
		}
		Skript.debug("Loading for Minecraft " + minecraftVersion);

		// Check that MC version is supported
		if (!isRunningMinecraft(1, 9)) {
			// Prevent loading when not running at least Minecraft 1.9
			Skript.error("This version of Skript does not work with Minecraft " + minecraftVersion + " and requires Minecraft 1.9.4+");
			Skript.error("You probably want Skript 2.2 or 2.1 (Google to find where to get them)");
			Skript.error("Note that those versions are, of course, completely unsupported!");
			return false;
		}

		// Check that current server platform is somewhat supported
		serverPlatform = getServerPlatform();
		Skript.debug("Server platform: " + serverPlatform);
		if (!serverPlatform.works) {
			Skript.error("It seems that this server platform (" + serverPlatform.name + ") does not work with Skript.");
			if (SkriptConfig.allowUnsafePlatforms.value()) {
				Skript.error("However, you have chosen to ignore this. Skript will probably still not work.");
			} else {
				Skript.error("To prevent potentially unsafe behaviour, Skript has been disabled.");
				Skript.error("You may re-enable it by adding a configuration option 'allow unsafe platforms: true'");
				Skript.error("Note that it is unlikely that Skript works correctly even if you do so.");
				Skript.error("A better idea would be to install Paper or Spigot in place of your current server.");
				return false;
			}
		} else if (!serverPlatform.supported) {
			Skript.warning("This server platform (" + serverPlatform.name + ") is not supported by Skript.");
			Skript.warning("It will still probably work, but if it does not, you are on your own.");
			Skript.warning("Skript officially supports Paper and Spigot.");
		}

		// If nothing got triggered, everything is probably ok
		return true;
	}

	private static final Set<Class<? extends Hook<?>>> disabledHookRegistrations = new HashSet<>();
	private static boolean finishedLoadingHooks = false;

	/**
	 * Checks whether a hook has been enabled.
	 * @param hook The hook to check.
	 * @return Whether the hook is enabled.
	 * @see #disableHookRegistration(Class[])
	 */
	public static boolean isHookEnabled(Class<? extends Hook<?>> hook) {
		return !disabledHookRegistrations.contains(hook);
	}

	/**
	 * @return whether hooks have been loaded,
	 * and if {@link #disableHookRegistration(Class[])} won't error because of this.
	 */
	public static boolean isFinishedLoadingHooks() {
		return finishedLoadingHooks;
	}

	/**
	 * Disables the registration for the given hook classes. If Skript has been enabled, this method
	 * will throw an API exception. It should be used in something like {@link JavaPlugin#onLoad()}.
	 * @param hooks The hooks to disable the registration of.
	 * @see #isHookEnabled(Class)
	 */
	@SafeVarargs
	public static void disableHookRegistration(Class<? extends Hook<?>>... hooks) {
		if (finishedLoadingHooks) { // Hooks have been registered if Skript is enabled
			throw new SkriptAPIException("Disabling hooks is not possible after Skript has been enabled!");
		}
		Collections.addAll(disabledHookRegistrations, hooks);
	}

	/**
	 * The folder containing all Scripts.
	 * Never reference this field directly. Use {@link #getScriptsFolder()}.
	 */
	private File scriptsFolder;

	/**
	 * @return The manager for experimental, optional features.
	 */
	public static ExperimentRegistry experiments() {
		return experimentRegistry;
	}

	/**
	 * @return The folder containing all Scripts.
	 */
	public File getScriptsFolder() {
		if (!scriptsFolder.isDirectory())
			//noinspection ResultOfMethodCallIgnored
			scriptsFolder.mkdirs();
		return scriptsFolder;
	}


	// ================ RUNTIME ERRORS ================
	public static RuntimeErrorManager getRuntimeErrorManager() {
		return RuntimeErrorManager.getInstance();
	}
	// =================================================

	@Override
	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);
		if (disabled) {
			Skript.error(m_invalid_reload.toString());
			setEnabled(false);
			return;
		}

		handleJvmArguments(); // JVM arguments

		version = new Version("" + getDescription().getVersion()); // Skript version

		// Start the updater
		// Note: if config prohibits update checks, it will NOT do network connections
		try {
			this.updater = new SkriptUpdater();
		} catch (Exception e) {
			Skript.exception(e, "Update checker could not be initialized.");
		}

		if (!getDataFolder().isDirectory())
			getDataFolder().mkdirs();

		scriptsFolder = new File(getDataFolder(), SCRIPTSFOLDER);
		File config = new File(getDataFolder(), "config.sk");
		File features = new File(getDataFolder(), "features.sk");
		File lang = new File(getDataFolder(), "lang");
		File aliasesFolder = new File(getDataFolder(), "aliases");
		if (!scriptsFolder.isDirectory() || !config.exists() || !features.exists() || !lang.exists() || !aliasesFolder.exists()) {
			ZipFile f = null;
			try {
				boolean populateExamples = false;
				if (!scriptsFolder.isDirectory()) {
					if (!scriptsFolder.mkdirs())
						throw new IOException("Could not create the directory " + scriptsFolder);
					populateExamples = true;
				}

				boolean populateLanguageFiles = false;
				if (!lang.isDirectory()) {
					if (!lang.mkdirs())
						throw new IOException("Could not create the directory " + lang);
					populateLanguageFiles = true;
				}

				if (!aliasesFolder.isDirectory()) {
					if (!aliasesFolder.mkdirs())
						throw new IOException("Could not create the directory " + aliasesFolder);
				}
				
				f = new ZipFile(getFile());
				for (ZipEntry e : new EnumerationIterable<ZipEntry>(f.entries())) {
					if (e.isDirectory())
						continue;
					File saveTo = null;
					if (populateExamples && e.getName().startsWith(SCRIPTSFOLDER + "/")) {
						String fileName = e.getName().substring(e.getName().indexOf("/") + 1);
						// All example scripts must be disabled for jar security.
						if (!fileName.startsWith(ScriptLoader.DISABLED_SCRIPT_PREFIX))
							fileName = ScriptLoader.DISABLED_SCRIPT_PREFIX + fileName;
						saveTo = new File(scriptsFolder, fileName);
					} else if (populateLanguageFiles
							&& e.getName().startsWith("lang/")
							&& !e.getName().endsWith("default.lang")) {
						String fileName = e.getName().substring(e.getName().lastIndexOf("/") + 1);
						saveTo = new File(lang, fileName);
					} else if (e.getName().equals("config.sk")) {
						if (!config.exists())
							saveTo = config;
//					} else if (e.getName().startsWith("aliases-") && e.getName().endsWith(".sk") && !e.getName().contains("/")) {
//						File af = new File(getDataFolder(), e.getName());
//						if (!af.exists())
//							saveTo = af;
					} else if (e.getName().startsWith("features.sk")) {
						if (!features.exists())
							saveTo = features;
					}
					if (saveTo != null) {
						InputStream in = f.getInputStream(e);
						try {
							assert in != null;
							FileUtils.save(in, saveTo);
						} finally {
							in.close();
						}
					}
				}
				info("Successfully generated the config and the example scripts.");
			} catch (ZipException ignored) {} catch (IOException e) {
				error("Error generating the default files: " + ExceptionUtils.toString(e));
			} finally {
				if (f != null) {
					try {
						f.close();
					} catch (IOException ignored) {}
				}
			}
		}

		// initialize the modern Skript instance
		skript = org.skriptlang.skript.Skript.of(getClass(), getName());
		unmodifiableSkript = new ModernSkriptBridge.SpecialUnmodifiableSkript(skript);
		skript.localizer().setSourceDirectories("lang",
				getDataFolder().getAbsolutePath() + File.separatorChar + "lang");
		// initialize the old Skript SkriptAddon instance
		getAddonInstance();

		experimentRegistry = new ExperimentRegistry(this);
		Feature.registerAll(getAddonInstance(), experimentRegistry);

		// Load classes which are always safe to use
		new JavaClasses(); // These may be needed in configuration

		// Check server software, Minecraft version, etc.
		if (!checkServerPlatform()) {
			disabled = true; // Nothing was loaded, nothing needs to be unloaded
			setEnabled(false); // Cannot continue; user got errors in console to tell what happened
			return;
		}

		// And then not-so-safe classes
		Throwable classLoadError = null;
		try {
			new SkriptClasses();
			new BukkitClasses();
		} catch (Throwable e) {
			classLoadError = e;
		}

		// Warn about pausing
		if (Skript.methodExists(Server.class, "getPauseWhenEmptyTime")) {
			int pauseThreshold = getServer().getPauseWhenEmptyTime();
			if (pauseThreshold > -1) {
				Skript.warning("Minecraft server pausing is enabled!");
				Skript.warning("Scripts that interact with the world or entities may not work as intended when the server is paused and may crash your server.");
				Skript.warning("Consider setting 'pause-when-empty-seconds' to -1 in server.properties to make sure you don't encounter any issues.");
			}
		}


		// Config must be loaded after Java and Skript classes are parseable
		// ... but also before platform check, because there is a config option to ignore some errors
		SkriptConfig.load();

		// Register the runtime error refresh after loading, so we can do the first instantiation manually.
		SkriptConfig.eventRegistry().register(SkriptConfig.ReloadEvent.class, RuntimeErrorManager::refresh);

		// init runtime error manager and add bukkit consumer.
		RuntimeErrorManager.refresh();
		getRuntimeErrorManager().addConsumer(new BukkitRuntimeErrorConsumer());

		// Now override the verbosity if test mode is enabled
		if (TestMode.VERBOSITY != null)
			SkriptLogger.setVerbosity(Verbosity.valueOf(TestMode.VERBOSITY));

		// Use the updater, now that it has been configured to (not) do stuff
		if (updater != null) {
			CommandSender console = Bukkit.getConsoleSender();
			assert console != null;
			assert updater != null;
			updater.updateCheck(console);
		}

		// If loading can continue (platform ok), check for potentially thrown error
		if (classLoadError != null) {
			exception(classLoadError);
			setEnabled(false);
			return;
		}

		PluginCommand skriptCommand = getCommand("skript");
		assert skriptCommand != null; // It is defined, unless build is corrupted or something like that
		skriptCommand.setExecutor(new SkriptCommand());
		skriptCommand.setTabCompleter(new SkriptCommandTabCompleter());

		// Load Bukkit stuff. It is done after platform check, because something might be missing!
		new BukkitEventValues();

		new DefaultComparators();
		new DefaultConverters();
		new DefaultFunctions();
		new DefaultOperations();

		ChatMessages.registerListeners();

		try {
			getAddonInstance().loadClasses("ch.njol.skript",
				"conditions", "effects", "events", "expressions", "entity", "sections", "structures");
			getAddonInstance().loadClasses("org.skriptlang.skript.bukkit", "misc");
			// todo: become proper module once registry api is merged
			FishingModule.load();
			BreedingModule.load();
			DisplayModule.load();
			InputModule.load();
			TagModule.load();
			FurnaceModule.load();
			LootTableModule.load();
			skript.loadModules(new DamageSourceModule());
		} catch (final Exception e) {
			exception(e, "Could not load required .class files: " + e.getLocalizedMessage());
			setEnabled(false);
			return;
		}

		// todo: remove completely 2.11 or 2.12
		CompletableFuture<Boolean> aliases = Aliases.loadAsync();

		Commands.registerListeners();

		if (logNormal())
			info(" " + Language.get("skript.copyright"));

		final long tick = testing() ? Bukkit.getWorlds().get(0).getFullTime() : 0;
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void run() {
				assert Bukkit.getWorlds().get(0).getFullTime() == tick;

				// Load hooks from Skript jar
				try {
					try (JarFile jar = new JarFile(getFile())) {
						for (JarEntry e : new EnumerationIterable<>(jar.entries())) {
							if (e.getName().startsWith("ch/njol/skript/hooks/") && e.getName().endsWith("Hook.class") && StringUtils.count("" + e.getName(), '/') <= 5) {
								final String c = e.getName().replace('/', '.').substring(0, e.getName().length() - ".class".length());
								try {
									Class<?> hook = Class.forName(c, true, getClassLoader());
									if (Hook.class.isAssignableFrom(hook) && !Modifier.isAbstract(hook.getModifiers()) && isHookEnabled((Class<? extends Hook<?>>) hook)) {
										hook.getDeclaredConstructor().setAccessible(true);
										hook.getDeclaredConstructor().newInstance();
									}
								} catch (ClassNotFoundException ex) {
									Skript.exception(ex, "Cannot load class " + c);
								} catch (ExceptionInInitializerError err) {
									Skript.exception(err.getCause(), "Class " + c + " generated an exception while loading");
								} catch (Exception ex) {
									Skript.exception(ex, "Exception initializing hook: " + c);
								}
							}
						}
					}
				} catch (IOException e) {
					error("Error while loading plugin hooks" + (e.getLocalizedMessage() == null ? "" : ": " + e.getLocalizedMessage()));
					Skript.exception(e);
				}
				finishedLoadingHooks = true;

				try {
					aliases.get(); // wait for aliases to load
				} catch (InterruptedException | ExecutionException e) {
					exception(e, "Could not load aliases concurrently");
				}

				if (TestMode.ENABLED) {
					info("Preparing Skript for testing...");
					tainted = true;
					try {
						getAddonInstance().loadClasses("ch.njol.skript.test.runner");
						if (TestMode.JUNIT)
							getAddonInstance().loadClasses("org.skriptlang.skript.test.junit.registration");
					} catch (IOException e) {
						Skript.exception("Failed to load testing environment.");
						Bukkit.getServer().shutdown();
					}
				}

				stopAcceptingRegistrations();

				Documentation.generate(); // TODO move to test classes?

				// Variable loading
				if (logNormal())
					info("Loading variables...");
				long vls = System.currentTimeMillis();

				LogHandler h = SkriptLogger.startLogHandler(new ErrorDescLogHandler() {
					@Override
					public LogResult log(final LogEntry entry) {
						super.log(entry);
						if (entry.level.intValue() >= Level.SEVERE.intValue()) {
							logEx(entry.message); // no [Skript] prefix
							return LogResult.DO_NOT_LOG;
						} else {
							return LogResult.LOG;
						}
					}

					@Override
					protected void beforeErrors() {
						logEx();
						logEx("===!!!=== Skript variable load error ===!!!===");
						logEx("Unable to load (all) variables:");
					}

					@Override
					protected void afterErrors() {
						logEx();
						logEx("Skript will work properly, but old variables might not be available at all and new ones may or may not be saved until Skript is able to create a backup of the old file and/or is able to connect to the database (which requires a restart of Skript)!");
						logEx();
					}
				});

				try (CountingLogHandler c = new CountingLogHandler(SkriptLogger.SEVERE).start()) {
					if (!Variables.load())
						if (c.getCount() == 0)
							error("(no information available)");
				} finally {
					h.stop();
				}

				long vld = System.currentTimeMillis() - vls;
				if (logNormal())
					info("Loaded " + Variables.numVariables() + " variables in " + ((vld / 100) / 10.) + " seconds");

				// Skript initialization done
				debug("Early init done");

				if (TestMode.ENABLED) {
					if (TestMode.DEV_MODE) {
						runTests(); // Dev mode doesn't need a delay
					} else {
						PaperLib.getChunkAtAsync(Bukkit.getWorlds().get(0), 100, 100).thenRun(() -> runTests());
					}
				}

				Skript.metrics = new Metrics(Skript.getInstance(), 722); // 722 is our bStats plugin ID
				SkriptMetrics.setupMetrics(Skript.metrics);

				/*
				 * Start loading scripts
				 */
				Date start = new Date();
				CountingLogHandler logHandler = new CountingLogHandler(Level.SEVERE);

				File scriptsFolder = getScriptsFolder();
				ScriptLoader.updateDisabledScripts(scriptsFolder.toPath());
				ScriptLoader.loadScripts(scriptsFolder, logHandler)
					.thenAccept(scriptInfo -> {
						try {
							if (logHandler.getCount() == 0)
								Skript.info(m_no_errors.toString());
							if (scriptInfo.files == 0)
								Skript.warning(m_no_scripts.toString());
							if (Skript.logNormal() && scriptInfo.files > 0)
								Skript.info(m_scripts_loaded.toString(
									scriptInfo.files,
									scriptInfo.structures,
									start.difference(new Date())
								));

							Skript.info(m_finished_loading.toString());

							// EvtSkript.onSkriptStart should be called on main server thread
							if (!ScriptLoader.isAsync()) {
								EvtSkript.onSkriptStart();

								// Suppresses the "can't keep up" warning after loading all scripts
								// Only for non-asynchronous loading
								Filter filter = record -> {
									if (record == null)
										return false;
									return record.getMessage() == null
										|| !record.getMessage().toLowerCase(Locale.ENGLISH).startsWith("can't keep up!");
								};
								BukkitLoggerFilter.addFilter(filter);
								Bukkit.getScheduler().scheduleSyncDelayedTask(
									Skript.this,
									() -> BukkitLoggerFilter.removeFilter(filter),
									1);
							} else {
								Bukkit.getScheduler().scheduleSyncDelayedTask(Skript.this,
									EvtSkript::onSkriptStart);
							}
						} catch (Exception e) {
							// Something went wrong, we need to make sure the exception is printed
							throw Skript.exception(e);
						}
					});

			}
		});

		if (!TestMode.ENABLED) {
			Bukkit.getPluginManager().registerEvents(new JoinUpdateNotificationListener(), this);
		}

		// Send a warning to console when the plugin is reloaded
		Bukkit.getPluginManager().registerEvents(new ServerReloadListener(), this);

		// Tell Timings that we are here!
		SkriptTimings.setSkript(this);
	}

	private static class ServerReloadListener implements Listener {

		@EventHandler
		public void onServerReload(ServerLoadEvent event) {
			if ((event.getType() != ServerLoadEvent.LoadType.RELOAD))
				return;

			for (OfflinePlayer player : Bukkit.getOperators()) {
				if (player.isOnline()) {
					player.getPlayer().sendMessage(ChatColor.YELLOW + getWarningMessage());
					player.getPlayer().sendMessage(ChatColor.YELLOW + getRestartMessage());
				}
			}

			Skript.warning(getWarningMessage());
			Skript.warning(getRestartMessage());
		}
	}

	private class JoinUpdateNotificationListener implements Listener {

		@EventHandler
		public void onJoin(PlayerJoinEvent event) {
			if (!event.getPlayer().hasPermission("skript.admin"))
				return;

			new Task(Skript.this, 0) {
				@Override
				public void run() {
					Player player = event.getPlayer();
					SkriptUpdater updater = getUpdater();

					// Don't actually check for updates to avoid breaking GitHub rate limit
					if (updater == null || updater.getReleaseStatus() != ReleaseStatus.OUTDATED)
						return;

					// Last check indicated that an update is available
					UpdateManifest update = updater.getUpdateManifest();

					if (update == null)
						return;

					Skript.info(player, SkriptUpdater.m_update_available.toString(update.id, Skript.getVersion()));
					player.spigot().sendMessage(BungeeConverter.convert(ChatMessages.parseToArray(
						"Download it at: <aqua><u><link:" + update.downloadUrl + ">" + update.downloadUrl)));
				}
			};
		}
  	}

	private void runTests() {
		info("Skript testing environment enabled, starting...");

		// Delay is in Minecraft ticks.
		AtomicLong shutdownDelay = new AtomicLong(0);
		List<Class<?>> asyncTests = new ArrayList<>();
		CompletableFuture<Void> onAsyncComplete = CompletableFuture.completedFuture(null);

		if (TestMode.GEN_DOCS) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "skript gen-docs");
		} else if (TestMode.DEV_MODE) { // Developer controlled environment.
			info("Test development mode enabled. Test scripts are at " + TestMode.TEST_DIR);
			return;
		} else {
			info("Loading all tests from " + TestMode.TEST_DIR);

			// Treat parse errors as fatal testing failure
			TestingLogHandler errorCounter = new TestingLogHandler(Level.SEVERE);
			try {
				errorCounter.start();

				// load example scripts (cleanup after)
				ScriptLoader.loadScripts(new File(getScriptsFolder(), "-examples" + File.separator), errorCounter);
				// unload these as to not interfere with the tests
				ScriptLoader.unloadScripts(ScriptLoader.getLoadedScripts());

				// load test directory scripts
				ScriptLoader.loadScripts(TestMode.TEST_DIR.toFile(), errorCounter);
			} finally {
				errorCounter.stop();
			}

			Bukkit.getPluginManager().callEvent(new SkriptTestEvent());
			if (errorCounter.getCount() > 0) {
				TestTracker.testStarted("parse scripts");
				TestTracker.testFailed(errorCounter.getCount() + " error(s) found");
			}
			if (errored) { // Check for exceptions thrown while script was executing
				TestTracker.testStarted("run scripts");
				TestTracker.testFailed("exception was thrown during execution");
			}
			if (TestMode.JUNIT) {
				AtomicLong milliseconds = new AtomicLong(0),
					tests = new AtomicLong(0), fails = new AtomicLong(0),
					ignored = new AtomicLong(0), size = new AtomicLong(0);

				info("Running sync JUnit tests...");
				try {
					// Search for all test classes
					Set<Class<?>> classes = new HashSet<>();
					ClassLoader.builder()
						.addSubPackages("org.skriptlang.skript", "ch.njol.skript")
						.filter(fqn -> fqn.endsWith("Test"))
						.initialize(true)
						.deep(true)
						.forEachClass(clazz -> {
							if (clazz.isAnonymousClass() || clazz.isLocalClass())
								return;
							classes.add(clazz);
						})
						.build()
						.loadClasses(Skript.class, getFile());
					// remove some known non-tests that get picked up
					classes.remove(SkriptJUnitTest.class);
					classes.remove(SkriptAsyncJUnitTest.class);

					size.set(classes.size());
					for (Class<?> clazz : classes) {
						if (SkriptAsyncJUnitTest.class.isAssignableFrom(clazz)) {
							asyncTests.add(clazz); // do these later, all together
							continue;
						}

						runTest(clazz, shutdownDelay, tests, milliseconds, ignored, fails);
					}
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
						 InvocationTargetException | NoSuchMethodException | SecurityException e) {
					Skript.exception(e, "Failed to initalize test JUnit classes.");
				}
				if (ignored.get() > 0)
					Skript.warning("There were " + ignored + " ignored test cases! This can mean they are not properly setup in order in that class!");

				onAsyncComplete = CompletableFuture.runAsync(() -> {
					info("Running async JUnit tests...");
					try {
						for (Class<?> clazz : asyncTests) {
							runTest(clazz, shutdownDelay, tests, milliseconds, ignored, fails);
						}
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
							 InvocationTargetException | NoSuchMethodException | SecurityException e) {
						Skript.exception(e, "Failed to initalize test JUnit classes.");
					}
					if (ignored.get() > 0)
						Skript.warning("There were " + ignored + " ignored test cases! " +
							"This can mean they are not properly setup in order in that class!");

					info("Completed " + tests + " JUnit tests in " + size + " classes with " + fails +
						" failures in " + milliseconds + " milliseconds.");
				});
			}
		}

		onAsyncComplete.thenRun(() -> {
			double display = shutdownDelay.get() / 20.0;
			info("Testing done, shutting down the server in " + display + " second" + (display == 1 ? "" : "s") + "...");

			// Delay server shutdown to stop the server from crashing because the current tick takes a long time due to all the tests
			Bukkit.getScheduler().runTaskLater(Skript.this, () -> {
				info("Shutting down server.");
				if (TestMode.JUNIT && !EffObjectives.isJUnitComplete())
					EffObjectives.fail();

				info("Collecting results to " + TestMode.RESULTS_FILE);
				String results = new GsonBuilder()
					.setPrettyPrinting() // Easier to read lines
					.disableHtmlEscaping() // Fixes issue with "'" character in test strings going unicode
					.create().toJson(TestTracker.collectResults());
				try {
					Files.write(TestMode.RESULTS_FILE, results.getBytes(StandardCharsets.UTF_8));
				} catch (IOException e) {
					Skript.exception(e, "Failed to write test results.");
				}

				Bukkit.getServer().shutdown();
			}, shutdownDelay.get());
		});
	}

	private void runTest(Class<?> clazz, AtomicLong shutdownDelay, AtomicLong tests,
						 AtomicLong milliseconds, AtomicLong ignored, AtomicLong fails)
		throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		String test = clazz.getName();
		SkriptJUnitTest.setCurrentJUnitTest(test);
		SkriptJUnitTest.setShutdownDelay(0);

		info("Running JUnit test '" + test + "'");
		Result result = JUnitCore.runClasses(clazz);
		TestTracker.testStarted("JUnit: '" + test + "'");

		/*
		 * Usage of @After is pointless if the JUnit class requires delay. As the @After will happen instantly.
		 * The JUnit must override the 'cleanup' method to avoid Skript automatically cleaning up the test data.
		 */
		boolean overrides = false;
		for (Method method : clazz.getDeclaredMethods()) {
			if (!method.isAnnotationPresent(After.class))
				continue;
			if (SkriptJUnitTest.getShutdownDelay() > 1)
				warning("Methods annotated with @After in happen instantaneously, and '" + test + "' requires a delay. Do test cleanup in the junit script file or 'cleanup' method.");
			if (method.getName().equals("cleanup"))
				overrides = true;
		}
		if (SkriptJUnitTest.getShutdownDelay() > 1 && !overrides)
			error("The JUnit class '" + test + "' does not override the method 'cleanup', thus the test data will instantly be cleaned up " +
				"despite requiring a longer shutdown time: " + SkriptJUnitTest.getShutdownDelay());

		shutdownDelay.set(Math.max(shutdownDelay.get(), SkriptJUnitTest.getShutdownDelay()));
		tests.getAndAdd(result.getRunCount());
		milliseconds.getAndAdd(result.getRunTime());
		ignored.getAndAdd(result.getIgnoreCount());
		fails.getAndAdd(result.getFailureCount());

		// If JUnit failures are present, add them to the TestTracker.
		for (Failure failure : result.getFailures()) {
			String message = failure.getMessage() == null ? "" : " " + failure.getMessage();
			TestTracker.JUnitTestFailed(test, message);
			Skript.exception(failure.getException(), "JUnit test '" + failure.getTestHeader() + " failed.");
		}

		if (SkriptJUnitTest.class.isAssignableFrom(clazz) &&
			!SkriptAsyncJUnitTest.class.isAssignableFrom(clazz)) // can't access blocks, entities async
			((SkriptJUnitTest) clazz.getConstructor().newInstance()).cleanup();
		SkriptJUnitTest.clearJUnitTest();
	}

	/**
	 * Handles -Dskript.stuff command line arguments.
	 */
	private void handleJvmArguments() {
		Path folder = getDataFolder().toPath();

		/*
		 * Burger is a Python application that extracts data from Minecraft.
		 * Datasets for most common versions are available for download.
		 * Skript uses them to provide minecraft:material to Bukkit
		 * Material mappings on Minecraft 1.12 and older.
		 */
		String burgerEnabled = System.getProperty("skript.burger.enable");
		if (burgerEnabled != null) {
			tainted = true;
			String version = System.getProperty("skript.burger.version");
			String burgerInput;
			if (version == null) { // User should have provided JSON file path
				String inputFile = System.getProperty("skript.burger.file");
				if (inputFile == null) {
					Skript.exception("burger enabled but skript.burger.file not provided");
					return;
				}
				try {
					burgerInput = new String(Files.readAllBytes(Paths.get(inputFile)), StandardCharsets.UTF_8);
				} catch (IOException e) {
					Skript.exception(e);
					return;
				}
			} else { // Try to download Burger dataset for this version
				try {
					Path data = folder.resolve("burger-" + version + ".json");
					if (!Files.exists(data)) {
						URL url = new URL("https://pokechu22.github.io/Burger/" + version + ".json");
						try (InputStream is = url.openStream()) {
							Files.copy(is, data);
						}
					}
					burgerInput = new String(Files.readAllBytes(data), StandardCharsets.UTF_8);
				} catch (IOException e) {
					Skript.exception(e);
					return;
				}
			}

			// Use BurgerHelper to create some mappings, then dump them as JSON
			try {
				BurgerHelper burger = new BurgerHelper(burgerInput);
				Map<String,Material> materials = burger.mapMaterials();
				Map<Integer,Material> ids = BurgerHelper.mapIds();

				Gson gson = new Gson();
				Files.write(folder.resolve("materials_mappings.json"), gson.toJson(materials)
						.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
				Files.write(folder.resolve("id_mappings.json"), gson.toJson(ids)
						.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
			} catch (IOException e) {
				Skript.exception(e);
			}
		}
	}

	public static Version getMinecraftVersion() {
		return minecraftVersion;
	}

	/**
	 * @return Whether this server is running CraftBukkit
	 */
	public static boolean isRunningCraftBukkit() {
		return serverPlatform == ServerPlatform.BUKKIT_CRAFTBUKKIT;
	}

	/**
	 * @return Whether this server is running Minecraft <tt>major.minor</tt> or higher
	 */
	public static boolean isRunningMinecraft(final int major, final int minor) {
		if (minecraftVersion.compareTo(UNKNOWN_VERSION) == 0) { // Make sure minecraftVersion is properly assigned.
			updateMinecraftVersion();
		}
		return minecraftVersion.compareTo(major, minor) >= 0;
	}

	public static boolean isRunningMinecraft(final int major, final int minor, final int revision) {
		if (minecraftVersion.compareTo(UNKNOWN_VERSION) == 0) {
			updateMinecraftVersion();
		}
		return minecraftVersion.compareTo(major, minor, revision) >= 0;
	}

	public static boolean isRunningMinecraft(final Version v) {
		if (minecraftVersion.compareTo(UNKNOWN_VERSION) == 0) {
			updateMinecraftVersion();
		}
		return minecraftVersion.compareTo(v) >= 0;
	}

	/**
	 * Tests whether a given class exists in the classpath.
	 *
	 * @param className The {@link Class#getCanonicalName() canonical name} of the class
	 * @return Whether the given class exists.
	 */
	public static boolean classExists(final String className) {
		try {
			Class.forName(className);
			return true;
		} catch (final ClassNotFoundException e) {
			return false;
		}
	}

	/**
	 * Tests whether a method exists in the given class.
	 *
	 * @param c The class
	 * @param methodName The name of the method
	 * @param parameterTypes The parameter types of the method
	 * @return Whether the given method exists.
	 */
	public static boolean methodExists(final Class<?> c, final String methodName, final Class<?>... parameterTypes) {
		try {
			c.getDeclaredMethod(methodName, parameterTypes);
			return true;
		} catch (final NoSuchMethodException e) {
			return false;
		} catch (final SecurityException e) {
			return false;
		}
	}

	/**
	 * Tests whether a method exists in the given class, and whether the return type matches the expected one.
	 * <p>
	 * Note that this method doesn't work properly if multiple methods with the same name and parameters exist but have different return types.
	 *
	 * @param c The class
	 * @param methodName The name of the method
	 * @param parameterTypes The parameter types of the method
	 * @param returnType The expected return type
	 * @return Whether the given method exists.
	 */
	public static boolean methodExists(final Class<?> c, final String methodName, final Class<?>[] parameterTypes, final Class<?> returnType) {
		try {
			final Method m = c.getDeclaredMethod(methodName, parameterTypes);
			return m.getReturnType() == returnType;
		} catch (final NoSuchMethodException e) {
			return false;
		} catch (final SecurityException e) {
			return false;
		}
	}

	/**
	 * Tests whether a field exists in the given class.
	 *
	 * @param c The class
	 * @param fieldName The name of the field
	 * @return Whether the given field exists.
	 */
	public static boolean fieldExists(final Class<?> c, final String fieldName) {
		try {
			c.getDeclaredField(fieldName);
			return true;
		} catch (final NoSuchFieldException e) {
			return false;
		} catch (final SecurityException e) {
			return false;
		}
	}

	@Nullable
	static Metrics metrics;

	@Nullable
	public static Metrics getMetrics() {
		return metrics;
	}

	@SuppressWarnings({"null", "removal"})
	private final static Collection<Closeable> closeOnDisable = Collections.synchronizedCollection(new ArrayList<Closeable>());

	/**
	 * Registers a Closeable that should be closed when this plugin is disabled.
	 * <p>
	 * All registered Closeables will be closed after all scripts have been stopped.
	 *
	 * @param closeable
	 */
	@SuppressWarnings("removal")
	public static void closeOnDisable(final Closeable closeable) {
		closeOnDisable.add(closeable);
	}

	@SuppressWarnings("unused")
	@EventHandler
	public void onPluginDisable(PluginDisableEvent event) {
		Plugin plugin = event.getPlugin();
		PluginDescriptionFile descriptionFile = plugin.getDescription();
		if (descriptionFile.getDepend().contains("Skript") || descriptionFile.getSoftDepend().contains("Skript")) {
			// An addon being disabled, check if server is being stopped
			if (!isServerRunning()) {
				beforeDisable();
			}
		}
	}

	private static final boolean IS_STOPPING_EXISTS;
	@Nullable
	private static Method IS_RUNNING;
	@Nullable
	private static Object MC_SERVER;

	static {
		IS_STOPPING_EXISTS = methodExists(Server.class, "isStopping");

		if (!IS_STOPPING_EXISTS) {
			Server server = Bukkit.getServer();
			Class<?> clazz = server.getClass();

			Method serverMethod;
			try {
				serverMethod = clazz.getMethod("getServer");
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}

			try {
				MC_SERVER = serverMethod.invoke(server);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}

			try {
				// Spigot removed the mapping for this method in 1.18, so its back to obfuscated method
				// 1.19 mapping is u and 1.18 is v
				String isRunningMethod = "isRunning";

				if (Skript.isRunningMinecraft(1, 20, 5)) {
					isRunningMethod = "x";
				} else if (Skript.isRunningMinecraft(1, 20)) {
					isRunningMethod = "v";
				} else if (Skript.isRunningMinecraft(1, 19)) {
					isRunningMethod = "u";
				} else if (Skript.isRunningMinecraft(1, 18)) {
					isRunningMethod = "v";
				}
				IS_RUNNING = MC_SERVER.getClass().getMethod(isRunningMethod);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@SuppressWarnings("ConstantConditions")
	private boolean isServerRunning() {
		if (IS_STOPPING_EXISTS)
			return !Bukkit.getServer().isStopping();

		try {
			return (boolean) IS_RUNNING.invoke(MC_SERVER);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private void beforeDisable() {
		partDisabled = true;
		EvtSkript.onSkriptStop(); // TODO [code style] warn user about delays in Skript stop events
		ScriptLoader.unloadScripts(ScriptLoader.getLoadedScripts());
	}

	@Override
	@SuppressWarnings("removal")
	public void onDisable() {
		if (disabled)
			return;
		disabled = true;

		if (!partDisabled) {
			beforeDisable();
		}

		Bukkit.getScheduler().cancelTasks(this);

		for (Closeable c : closeOnDisable) {
			try {
				c.close();
			} catch (final Exception e) {
				Skript.exception(e, "An error occurred while shutting down.", "This might or might not cause any issues.");
			}
		}

		this.experimentRegistry = null;
	}

	// ================ CONSTANTS, OPTIONS & OTHER ================

	public final static String SCRIPTSFOLDER = "scripts";

	public static void outdatedError() {
		error("Skript v" + getInstance().getDescription().getVersion() + " is not fully compatible with Bukkit " + Bukkit.getVersion() + ". Some feature(s) will be broken until you update Skript.");
	}

	public static void outdatedError(final Exception e) {
		outdatedError();
		if (testing())
			e.printStackTrace();
	}

	/**
	 * A small value, useful for comparing doubles or floats.
	 * <p>
	 * E.g. to test whether two floating-point numbers are equal:
	 *
	 * <pre>
	 * Math.abs(a - b) &lt; Skript.EPSILON
	 * </pre>
	 *
	 * or whether a location is within a specific radius of another location:
	 *
	 * <pre>
	 * location.distanceSquared(center) - radius * radius &lt; Skript.EPSILON
	 * </pre>
	 *
	 * @see #EPSILON_MULT
	 */
	public final static double EPSILON = 1e-10;
	/**
	 * A value a bit larger than 1
	 *
	 * @see #EPSILON
	 */
	public final static double EPSILON_MULT = 1.00001;

	/**
	 * The maximum ID a block can have in Minecraft.
	 */
	public final static int MAXBLOCKID = 255;
	/**
	 * The maximum data value of Minecraft, i.e. Short.MAX_VALUE - Short.MIN_VALUE.
	 */
	public final static int MAXDATAVALUE = Short.MAX_VALUE - Short.MIN_VALUE;

	// TODO localise Infinity, -Infinity, NaN (and decimal point?)
	public static String toString(final double n) {
		return StringUtils.toString(n, SkriptConfig.numberAccuracy.value());
	}

	public final static UncaughtExceptionHandler UEH = new UncaughtExceptionHandler() {
		@Override
		public void uncaughtException(final @Nullable Thread t, final @Nullable Throwable e) {
			Skript.exception(e, "Exception in thread " + (t == null ? null : t.getName()));
		}
	};

	/**
	 * Creates a new Thread and sets its UncaughtExceptionHandler. The Thread is not started automatically.
	 */
	public static Thread newThread(final Runnable r, final String name) {
		final Thread t = new Thread(r, name);
		t.setUncaughtExceptionHandler(UEH);
		return t;
	}

	// ================ REGISTRATIONS ================

	private static boolean acceptRegistrations = true;

	public static boolean isAcceptRegistrations() {
		if (instance == null)
			throw new IllegalStateException("Skript was never loaded");
		return acceptRegistrations && instance.isEnabled();
	}

	public static void checkAcceptRegistrations() {
		if (!isAcceptRegistrations() && !Skript.testing())
			throw new SkriptAPIException("Registration can only be done during plugin initialization");
	}

	private static void stopAcceptingRegistrations() {
		Converters.createChainedConverters();
		acceptRegistrations = false;
		Classes.onRegistrationsStop();
	}

	// ================ ADDONS ================

	@Deprecated(since = "2.10.0", forRemoval = true)
	private static final Set<SkriptAddon> addons = new HashSet<>();

	/**
	 * Registers an addon to Skript. This is currently not required for addons to work, but the returned {@link SkriptAddon} provides useful methods for registering syntax elements
	 * and adding new strings to Skript's localization system (e.g. the required "types.[type]" strings for registered classes).
	 *
	 * @param plugin The plugin
	 */
	public static SkriptAddon registerAddon(JavaPlugin plugin) {
		checkAcceptRegistrations();
		SkriptAddon addon = new SkriptAddon(plugin);
		addons.add(addon);
		return addon;
	}

	public static @Nullable SkriptAddon getAddon(JavaPlugin plugin) {
		if (plugin == Skript.getInstance()) {
			return Skript.getAddonInstance();
		}
		for (SkriptAddon addon : getAddons()) {
			if (addon.plugin == plugin) {
				return addon;
			}
		}
		return null;
	}

	public static @Nullable SkriptAddon getAddon(String name) {
		if (name.equals(Skript.getInstance().getName())) {
			return Skript.getAddonInstance();
		}
		for (SkriptAddon addon : getAddons()) {
			if (addon.getName().equals(name)) {
				return addon;
			}
		}
		return null;
	}

	public static @Unmodifiable Collection<SkriptAddon> getAddons() {
		Set<SkriptAddon> addons = new HashSet<>(Skript.addons);
		addons.addAll(instance().addons().stream()
			.filter(addon -> addons.stream().noneMatch(oldAddon -> oldAddon.name().equals(addon.name())))
			.map(SkriptAddon::fromModern)
			.collect(Collectors.toSet())
		);
		return Collections.unmodifiableCollection(addons);
	}

	@Deprecated(since = "2.10.0", forRemoval = true)
	private static @Nullable SkriptAddon addon;

	/**
	 * @return A {@link SkriptAddon} representing Skript.
	 */
	public static SkriptAddon getAddonInstance() {
		if (addon == null) {
			addon = SkriptAddon.fromModern(instance());
		}
		return addon;
	}

	// ================ CONDITIONS & EFFECTS & SECTIONS ================

	private static final class BukkitOrigin implements SyntaxOrigin {

		private final String name;

		private BukkitOrigin(Plugin plugin) {
			this.name = plugin.getName();
		}

		@Override
		public String name() {
			return name;
		}

	}

	/**
	 * Attempts to create a SyntaxOrigin from a provided class.
	 */
	@ApiStatus.Internal
	@ApiStatus.Experimental
	public static SyntaxOrigin getSyntaxOrigin(Class<?> source) {
		JavaPlugin plugin;
		try {
			plugin = JavaPlugin.getProvidingPlugin(source);
		} catch (IllegalArgumentException e) { // Occurs when the method fails to determine the providing plugin
			return () -> source.getName();
		}
		SkriptAddon addon = getAddon(plugin);
		if (addon != null) {
			return SyntaxOrigin.of(addon);
		}
		return new BukkitOrigin(plugin);
	}

	/**
	 * Registers a {@link Condition}.
	 *
	 * @param conditionClass The condition's class
	 * @param patterns Skript patterns to match this condition
	 */
	public static <E extends Condition> void registerCondition(Class<E> conditionClass, String... patterns) throws IllegalArgumentException {
		registerCondition(conditionClass, ConditionType.COMBINED, patterns);
	}

	/**
	 * Registers a {@link Condition}.
	 *
	 * @param conditionClass The condition's class
	 * @param type The type of condition which affects its priority in the parsing search
	 * @param patterns Skript patterns to match this condition
	 */
	public static <E extends Condition> void registerCondition(Class<E> conditionClass, ConditionType type, String... patterns) throws IllegalArgumentException {
		checkAcceptRegistrations();
		skript.syntaxRegistry().register(SyntaxRegistry.CONDITION, SyntaxInfo.builder(conditionClass)
				.priority(type.priority())
				.origin(getSyntaxOrigin(conditionClass))
				.addPatterns(patterns)
				.build()
		);
	}

	/**
	 * Registers an {@link Effect}.
	 *
	 * @param effectClass The effect's class
	 * @param patterns Skript patterns to match this effect
	 */
	public static <E extends Effect> void registerEffect(Class<E> effectClass, String... patterns) throws IllegalArgumentException {
		checkAcceptRegistrations();
		skript.syntaxRegistry().register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(effectClass)
				.origin(getSyntaxOrigin(effectClass))
				.addPatterns(patterns)
				.build()
		);
	}

	/**
	 * Registers a {@link Section}.
	 *
	 * @param sectionClass The section's class
	 * @param patterns Skript patterns to match this section
	 * @see Section
	 */
	public static <E extends Section> void registerSection(Class<E> sectionClass, String... patterns) throws IllegalArgumentException {
		checkAcceptRegistrations();
		skript.syntaxRegistry().register(SyntaxRegistry.SECTION, SyntaxInfo.builder(sectionClass)
				.origin(getSyntaxOrigin(sectionClass))
				.addPatterns(patterns)
				.build()
		);
	}

	public static @Unmodifiable Collection<SyntaxElementInfo<? extends Statement>> getStatements() {
		return instance().syntaxRegistry()
				.syntaxes(SyntaxRegistry.STATEMENT).stream()
				.map(SyntaxElementInfo::<SyntaxElementInfo<Statement>, Statement>fromModern)
				.collect(Collectors.toUnmodifiableList());
	}

	public static @Unmodifiable Collection<SyntaxElementInfo<? extends Condition>> getConditions() {
		return instance().syntaxRegistry()
				.syntaxes(SyntaxRegistry.CONDITION).stream()
				.map(SyntaxElementInfo::<SyntaxElementInfo<Condition>, Condition>fromModern)
				.collect(Collectors.toUnmodifiableList());
	}

	public static @Unmodifiable Collection<SyntaxElementInfo<? extends Effect>> getEffects() {
		return instance().syntaxRegistry()
				.syntaxes(SyntaxRegistry.EFFECT).stream()
				.map(SyntaxElementInfo::<SyntaxElementInfo<Effect>, Effect>fromModern)
				.collect(Collectors.toUnmodifiableList());
	}

	public static @Unmodifiable Collection<SyntaxElementInfo<? extends Section>> getSections() {
		return instance().syntaxRegistry()
				.syntaxes(SyntaxRegistry.SECTION).stream()
				.map(SyntaxElementInfo::<SyntaxElementInfo<Section>, Section>fromModern)
				.collect(Collectors.toUnmodifiableList());
	}

	// ================ EXPRESSIONS ================

	/**
	 * Registers an expression.
	 *
	 * @param expressionType The expression's class
	 * @param returnType The superclass of all values returned by the expression
	 * @param type The expression's {@link ExpressionType type}. This is used to determine in which order to try to parse expressions.
	 * @param patterns Skript patterns that match this expression
	 * @throws IllegalArgumentException if returnType is not a normal class
	 */
	public static <E extends Expression<T>, T> void registerExpression(
		Class<E> expressionType, Class<T> returnType, ExpressionType type, String... patterns
	) throws IllegalArgumentException {
		checkAcceptRegistrations();
		skript.syntaxRegistry().register(SyntaxRegistry.EXPRESSION, SyntaxInfo.Expression.builder(expressionType, returnType)
				.priority(type.priority())
				.origin(getSyntaxOrigin(expressionType))
				.addPatterns(patterns)
				.build()
		);
	}

	public static Iterator<ExpressionInfo<?, ?>> getExpressions() {
		List<ExpressionInfo<?, ?>> list = new ArrayList<>();
		for (SyntaxInfo.Expression<?, ?> info : instance().syntaxRegistry().syntaxes(SyntaxRegistry.EXPRESSION))
			list.add((ExpressionInfo<?, ?>) SyntaxElementInfo.fromModern(info));
		return list.iterator();
	}

	public static Iterator<ExpressionInfo<?, ?>> getExpressions(Class<?>... returnTypes) {
		return new CheckedIterator<>(getExpressions(), info -> {
			if (info == null || info.returnType == Object.class)
				return true;
			for (Class<?> returnType : returnTypes) {
				assert returnType != null;
				if (Converters.converterExists(info.returnType, returnType))
					return true;
			}
			return false;
		});
	}

	// ================ EVENTS ================

	/**
	 * Registers an event.
	 *
	 * @param name Capitalised name of the event without leading "On" which is added automatically (Start the name with an asterisk to prevent this). Used for error messages and
	 *            the documentation.
	 * @param c The event's class
	 * @param event The Bukkit event this event applies to
	 * @param patterns Skript patterns to match this event
	 * @return A SkriptEventInfo representing the registered event. Used to generate Skript's documentation.
	 */
	@SuppressWarnings("unchecked")
	public static <E extends SkriptEvent> SkriptEventInfo<E> registerEvent(String name, Class<E> c, Class<? extends Event> event, String... patterns) {
		return registerEvent(name, c, new Class[] {event}, patterns);
	}

	/**
	 * Registers an event.
	 *
	 * @param name The name of the event, used for error messages
	 * @param eventClass The event's class
	 * @param events The Bukkit events this event applies to
	 * @param patterns Skript patterns to match this event
	 * @return A SkriptEventInfo representing the registered event. Used to generate Skript's documentation.
	 */
	@SuppressWarnings("ConstantConditions") // caused by bad array annotations
	public static <E extends SkriptEvent> SkriptEventInfo<E> registerEvent(
		String name, Class<E> eventClass, Class<? extends Event>[] events, String... patterns
	) {
		checkAcceptRegistrations();
		for (int i = 0; i < patterns.length; i++)
			patterns[i] = BukkitSyntaxInfos.fixPattern(patterns[i]);
		var legacy = new SkriptEventInfo.ModernSkriptEventInfo<>(name, patterns, eventClass, "", events);
		skript.syntaxRegistry().register(BukkitRegistryKeys.EVENT, legacy);
		return legacy;
	}

	public static <E extends Structure> void registerStructure(Class<E> structureClass, String... patterns) {
		checkAcceptRegistrations();
		skript.syntaxRegistry().register(SyntaxRegistry.STRUCTURE, SyntaxInfo.Structure.builder(structureClass)
				.origin(getSyntaxOrigin(structureClass))
				.addPatterns(patterns)
				.build()
		);
	}

	public static <E extends Structure> void registerSimpleStructure(Class<E> structureClass, String... patterns) {
		checkAcceptRegistrations();
		skript.syntaxRegistry().register(SyntaxRegistry.STRUCTURE, SyntaxInfo.Structure.builder(structureClass)
			.origin(getSyntaxOrigin(structureClass))
			.addPatterns(patterns)
			.nodeType(SyntaxInfo.Structure.NodeType.SIMPLE)
			.build()
		);
	}

	public static <E extends Structure> void registerStructure(
		Class<E> structureClass, EntryValidator entryValidator, String... patterns
	) {
		checkAcceptRegistrations();
		skript.syntaxRegistry().register(SyntaxRegistry.STRUCTURE, SyntaxInfo.Structure.builder(structureClass)
				.origin(getSyntaxOrigin(structureClass))
				.addPatterns(patterns)
				.entryValidator(entryValidator)
				.build()
		);
	}

	public static @Unmodifiable Collection<SkriptEventInfo<?>> getEvents() {
		return instance().syntaxRegistry()
				.syntaxes(BukkitRegistryKeys.EVENT).stream()
				.map(SyntaxElementInfo::<SkriptEventInfo<SkriptEvent>, SkriptEvent>fromModern)
				.collect(Collectors.toUnmodifiableList());
	}

	public static @Unmodifiable List<StructureInfo<? extends Structure>> getStructures() {
		return instance().syntaxRegistry()
				.syntaxes(SyntaxRegistry.STRUCTURE).stream()
				.map(SyntaxElementInfo::<StructureInfo<Structure>, Structure>fromModern)
				.collect(Collectors.toUnmodifiableList());
	}

	// ================ COMMANDS ================

	/**
	 * Dispatches a command with calling command events
	 *
	 * @param sender
	 * @param command
	 * @return Whether the command was run
	 */
	public static boolean dispatchCommand(final CommandSender sender, final String command) {
		try {
			if (sender instanceof Player) {
				final PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent((Player) sender, "/" + command);
				Bukkit.getPluginManager().callEvent(e);
				if (e.isCancelled() || !e.getMessage().startsWith("/"))
					return false;
				return Bukkit.dispatchCommand(e.getPlayer(), e.getMessage().substring(1));
			} else {
				final ServerCommandEvent e = new ServerCommandEvent(sender, command);
				Bukkit.getPluginManager().callEvent(e);
				if (e.getCommand().isEmpty() || e.isCancelled())
					return false;
				return Bukkit.dispatchCommand(e.getSender(), e.getCommand());
			}
		} catch (final Exception ex) {
			ex.printStackTrace(); // just like Bukkit
			return false;
		}
	}

	// ================ LOGGING ================

	public static boolean logNormal() {
		return SkriptLogger.log(Verbosity.NORMAL);
	}

	public static boolean logHigh() {
		return SkriptLogger.log(Verbosity.HIGH);
	}

	public static boolean logVeryHigh() {
		return SkriptLogger.log(Verbosity.VERY_HIGH);
	}

	public static boolean debug() {
		return SkriptLogger.debug();
	}

	public static boolean testing() {
		return debug() || Skript.class.desiredAssertionStatus();
	}

	public static boolean log(final Verbosity minVerb) {
		return SkriptLogger.log(minVerb);
	}

	public static void debug(final String info) {
		if (!debug())
			return;
		SkriptLogger.log(SkriptLogger.DEBUG, info);
	}

	/**
	 * Sends a debug message with formatted objects if {@link #debug()} returns true.
	 *
	 * @param message The message to send
	 * @param objects The objects to format the message with
	 * @see String#formatted(Object...)
	 */
	public static void debug(String message, Object... objects) {
		if (!debug())
			return;
		debug(message.formatted(objects));
	}

	/**
	 * @see SkriptLogger#log(Level, String)
	 */
	@SuppressWarnings("null")
	public static void info(final String info) {
		SkriptLogger.log(Level.INFO, info);
	}

	/**
	 * @see SkriptLogger#log(Level, String)
	 */
	@SuppressWarnings("null")
	public static void warning(final String warning) {
		SkriptLogger.log(Level.WARNING, warning);
	}

	/**
	 * @see SkriptLogger#log(Level, String)
	 */
	@SuppressWarnings("null")
	public static void error(final @Nullable String error) {
		if (error != null)
			SkriptLogger.log(Level.SEVERE, error);
	}

	/**
	 * Sends an error message with formatted objects.
	 *
	 * @param message The message to send
	 * @param objects The objects to format the message with
	 * @see String#formatted(Object...)
	 */
	public static void error(String message, Object... objects) {
		error(message.formatted(objects));
	}

	/**
	 * Use this in {@link Expression#init(Expression[], int, Kleenean, ch.njol.skript.lang.SkriptParser.ParseResult)} (and other methods that are called during the parsing) to log
	 * errors with a specific {@link ErrorQuality}.
	 *
	 * @param error
	 * @param quality
	 */
	public static void error(final String error, final ErrorQuality quality) {
		SkriptLogger.log(new LogEntry(SkriptLogger.SEVERE, quality, error));
	}

	private final static String EXCEPTION_PREFIX = "#!#! ";

	/**
	 * Used if something happens that shouldn't happen
	 *
	 * @param info Description of the error and additional information
	 * @return an EmptyStacktraceException to throw if code execution should terminate.
	 */
	public static EmptyStacktraceException exception(final String... info) {
		return exception(null, info);
	}

	public static EmptyStacktraceException exception(final @Nullable Throwable cause, final String... info) {
		return exception(cause, null, null, info);
	}

	public static EmptyStacktraceException exception(final @Nullable Throwable cause, final @Nullable Thread thread, final String... info) {
		return exception(cause, thread, null, info);
	}

	public static EmptyStacktraceException exception(final @Nullable Throwable cause, final @Nullable TriggerItem item, final String... info) {
		return exception(cause, null, item, info);
	}

	/**
	 * Maps Java packages of plugins to descriptions of said plugins.
	 * This is only done for plugins that depend or soft-depend on Skript.
	 */
	private static Map<String, PluginDescriptionFile> pluginPackages = new HashMap<>();
	private static boolean checkedPlugins = false;

	/**
	 * Set by Skript when doing something that users shouldn't do.
	 */
	private static boolean tainted = false;

	/**
	 * Set to true when an exception is thrown.
	 */
	private static boolean errored = false;

	/**
	 * Mark that an exception has occurred at some point during runtime.
	 * Only used for Skript's testing system.
	 */
	public static void markErrored() {
		errored = true;
	}

	/**
	 * Used if something happens that shouldn't happen
	 *
	 * @param cause exception that shouldn't occur
	 * @param info Description of the error and additional information
	 * @return an EmptyStacktraceException to throw if code execution should terminate.
	 */
	public static EmptyStacktraceException exception(@Nullable Throwable cause, @Nullable Thread thread, @Nullable TriggerItem item, String... info) {
		errored = true;

		// Avoid re-throwing the same exception
		if (cause instanceof EmptyStacktraceException) {
			return new EmptyStacktraceException();
		}

		// First error: gather plugin package information
		if (!checkedPlugins) {
			initializePluginPackages();
			checkedPlugins = true; // No need to do this next time
		}

		logErrorDetails(cause, info, thread, item);
		return new EmptyStacktraceException();
	}

	private static void initializePluginPackages() {
		for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
			if (plugin.getName().equals("Skript")) // Skip self
				continue;

			PluginDescriptionFile desc = plugin.getDescription();
			if (desc.getDepend().contains("Skript") || desc.getSoftDepend().contains("Skript")) {
				String mainClassPackage = getPackageName(desc.getMain());
				pluginPackages.put(mainClassPackage, desc);
				if (Skript.debug()) {
					Skript.info("Identified potential addon: " + desc.getFullName() + " (" + mainClassPackage + ")");
				}
			}
		}
	}

	private static String getPackageName(String qualifiedClassName) {
		int lastDotIndex = qualifiedClassName.lastIndexOf('.');
		return (lastDotIndex == -1) ? "" : qualifiedClassName.substring(0, lastDotIndex);
	}

	private static void logErrorDetails(@Nullable Throwable cause, String[] info, @Nullable Thread thread, @Nullable TriggerItem item) {
		String issuesUrl = "https://github.com/SkriptLang/Skript/issues";
		String downloadUrl = "https://github.com/SkriptLang/Skript/releases/latest"; //TODO grab this from the update checker

		logEx();
		logEx("[Skript] Severe Error:");
		logEx(info);
		logEx();

		Set<PluginDescriptionFile> stackPlugins = identifyPluginsInStackTrace(Thread.currentThread().getStackTrace());

		logPlatformSupportInfo(issuesUrl, downloadUrl, stackPlugins);

		logEx();
		logEx("Stack trace:");
		logStackTrace(cause);

		logEx();
		logVersionInfo();
		logEx();
		logCurrentState(thread, item);
		logEx("End of Error.");
		logEx();
	}

	private static Set<PluginDescriptionFile> identifyPluginsInStackTrace(StackTraceElement[] stackTrace) {
		Set<PluginDescriptionFile> stackPlugins = new HashSet<>();
		for (StackTraceElement element : stackTrace) {
			pluginPackages.entrySet().stream()
				.filter(entry -> element.getClassName().startsWith(entry.getKey()))
				.forEach(entry -> stackPlugins.add(entry.getValue()));
		}
		return stackPlugins;
	}


	private static void logPlatformSupportInfo(String issuesUrl, String downloadUrl, Set<PluginDescriptionFile> stackPlugins) {
		SkriptUpdater updater = Skript.getInstance().getUpdater();

		if (tainted) {
			logEx("Skript is running with developer command-line options. Consider disabling them if not a developer.");
		} else if (getInstance().getDescription().getVersion().contains("nightly")) {
			logEx("You're running a (buggy) nightly version of Skript. If this is not a test server, switch to a stable release.");
			logEx("Please report this bug to: " + issuesUrl);
		} else if (!serverPlatform.supported) {
			String supportedPlatforms = getSupportedPlatforms();
			logEx("Your server platform appears to be unsupported by Skript. Consider switching to one of the supported platforms (" + supportedPlatforms + ") for better compatibility.");
		} else if (updater != null && updater.getReleaseStatus() == ReleaseStatus.OUTDATED) {
			logEx("You're running an outdated version of Skript! Update to the latest version here: " + downloadUrl);
		} else {
			logEx("An unexpected error occurred with Skript. This issue is likely not your fault.");
			logExAddonInfo(issuesUrl, stackPlugins);
		}
	}

	private static String getSupportedPlatforms() {
		return Arrays.stream(ServerPlatform.values())
			.filter(platform -> platform.supported)
			.map(ServerPlatform::name)
			.collect(Collectors.joining(", "));
	}

	private static void logExAddonInfo(String issuesUrl, Set<PluginDescriptionFile> stackPlugins) {
		if (pluginPackages.isEmpty()) {
			logEx("Report the issue: " + issuesUrl);
		} else {
			logEx("You are using some plugins that alter how Skript works (addons).");
			if (stackPlugins.isEmpty()) {
				logEx("Full list of addons:");
				pluginPackages.values().forEach(desc -> logEx(getPluginDescription(desc)));
				logEx("We could not identify related addons, it might also be a Skript issue.");
			} else {
				logEx("The following plugins are likely related to this error:");
				stackPlugins.forEach(desc -> logEx(getPluginDescription(desc)));
			}
			logEx("Try temporarily removing the listed plugins one by one to identify the cause.");
			logEx("If removing a plugin resolves the issue, please report the problem to the plugin developer.");
		}
	}

	private static String getPluginDescription(PluginDescriptionFile desc) {
		String website = desc.getWebsite();
		return desc.getFullName() + (website != null && !website.isEmpty() ? " (" + website + ")" : "");
	}

	private static void logStackTrace(@Nullable Throwable cause) {
		if (cause == null || cause.getStackTrace().length == 0) {
			logEx("Warning: no/empty exception given, dumping current stack trace instead");
			cause = new Exception("EmptyStacktraceException cause");
		}
		while (cause != null) {
			logEx((cause == null ? "" : "Caused by: ") + cause.toString());
			for (StackTraceElement element : cause.getStackTrace()) {
				logEx("    at " + element.toString());
			}
			cause = cause.getCause();
		}
	}

	private static void logVersionInfo() {
		SkriptUpdater updater = Skript.getInstance().getUpdater();
		if (updater != null) {
			ReleaseStatus status = updater.getReleaseStatus();
			logEx("Skript: " + getVersion() + " (" + status.toString() + ")");
			ReleaseManifest current = updater.getCurrentRelease();
			logEx("    Flavor: " + current.flavor);
			logEx("    Date: " + current.date);
		} else {
			logEx("Skript: " + getVersion() + " (unknown; likely custom)");
		}
		logEx("Bukkit: " + Bukkit.getBukkitVersion());
		logEx("Minecraft: " + getMinecraftVersion());
		logEx("Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + ")");
		logEx("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + " " + System.getProperty("os.version"));
		logEx();
		logEx("Server platform: " + serverPlatform.name + (serverPlatform.supported ? "" : " (unsupported)"));
	}

	private static void logCurrentState(@Nullable Thread thread, @Nullable TriggerItem item) {
		logEx("Current node: " + SkriptLogger.getNode());
		logEx("Current item: " + (item == null ? "null" : item.toString(null, true)));
		if (item != null && item.getTrigger() != null) {
			Trigger trigger = item.getTrigger();
			Script script = trigger.getScript();
			logEx("Current trigger: " + trigger.toString(null, true) + " (" + (script == null ? "null" : script.getConfig().getFileName()) + ", line " + trigger.getLineNumber() + ")");
		}
		logEx("Thread: " + (thread == null ? Thread.currentThread() : thread).getName());
		logEx("Language: " + Language.getName());
		logEx("Link parse mode: " + ChatMessages.linkParseMode);
	}

	static void logEx() {
		SkriptLogger.LOGGER.severe(EXCEPTION_PREFIX);
	}

	static void logEx(final String... lines) {
		for (final String line : lines)
			SkriptLogger.LOGGER.severe(EXCEPTION_PREFIX + line);
	}

	private static final Message SKRIPT_PREFIX_MESSAGE = new Message("skript.prefix");

	public static String getSkriptPrefix() {
		return SKRIPT_PREFIX_MESSAGE.getValueOrDefault("<grey>[<gold>Skript<grey>] <reset>");
	}

	public static void info(final CommandSender sender, final String info) {
		sender.sendMessage(Utils.replaceEnglishChatStyles(getSkriptPrefix() + info));
	}

	/**
	 * @param message
	 * @param permission
	 * @see #adminBroadcast(String)
	 */
	public static void broadcast(final String message, final String permission) {
		Bukkit.broadcast(Utils.replaceEnglishChatStyles(getSkriptPrefix() + message), permission);
	}

	public static void adminBroadcast(final String message) {
		broadcast(message, "skript.admin");
	}

	/**
	 * Similar to {@link #info(CommandSender, String)} but no [Skript] prefix is added.
	 *
	 * @param sender
	 * @param info
	 */
	public static void message(final CommandSender sender, final String info) {
		sender.sendMessage(Utils.replaceEnglishChatStyles(info));
	}

	public static void error(final CommandSender sender, final String error) {
		sender.sendMessage(Utils.replaceEnglishChatStyles(getSkriptPrefix() + ChatColor.DARK_RED + error));
	}

	/**
	 * Gets the updater instance currently used by Skript.
	 * @return SkriptUpdater instance.
	 */
	@Nullable
	public SkriptUpdater getUpdater() {
		return updater;
	}

}
