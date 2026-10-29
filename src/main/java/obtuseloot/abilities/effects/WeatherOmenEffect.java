package obtuseloot.abilities.effects;

import obtuseloot.abilities.*;
import org.bukkit.World;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.function.Function;

/** Current weather state and server timers are observations, not a guarantee against later plugin changes. */
public final class WeatherOmenEffect extends MeasuredUtilityEffect {
    private final Function<World, Boolean> weatherCycle;
    public WeatherOmenEffect() { this(world -> world.getGameRuleValue(GameRule.DO_WEATHER_CYCLE)); }
    // API-double tests have no Paper RegistryAccess bootstrap. Only the registry-backed read is substituted.
    WeatherOmenEffect(Function<World, Boolean> weatherCycle) {
        super("weather-omen", 4, 200); this.weatherCycle = java.util.Objects.requireNonNull(weatherCycle);
    }
    @Override protected Report prepare(AbilityDefinition definition, AbilityEventContext context, Player player, double output) {
        var world = player.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) return null;
        var facts = new ArrayList<String>();
        boolean storm = world.hasStorm(), thunder = world.isThundering();
        facts.add(storm ? "rain or snow active" : "dry weather");
        facts.add("thunder " + thunder);
        Boolean cycle = weatherCycle.apply(world);
        facts.add("natural weather cycle " + (Boolean.TRUE.equals(cycle) ? "running" : "paused or unavailable"));
        var identities = new ArrayList<>(facts);
        if (Boolean.TRUE.equals(cycle)) {
            int clear = world.getClearWeatherDuration();
            int weather = Math.max(0, world.getWeatherDuration()), thunderTimer = Math.max(0, world.getThunderDuration());
            if (clear > 0) {
                facts.add("forced clear window " + clear + " ticks remaining");
                facts.add("natural rain and thunder held off during this window");
            } else {
                facts.add("current weather timer " + weather + " ticks remaining");
                facts.add("thunder timer " + thunderTimer + " ticks remaining");
            }
            // Countdown ticks do not create a new observation every time the same forecast is delivered.
            long time = world.getGameTime();
            identities.add(clear > 0 ? "clear until " + Math.addExact(time, clear) : "weather until " + Math.addExact(time, weather));
            identities.add(clear > 0 ? "natural thunder held off" : "thunder until " + Math.addExact(time, thunderTimer));
        }
        return information("Weather Omen", facts, identities, output, "weather:" + world.getUID());
    }
}
