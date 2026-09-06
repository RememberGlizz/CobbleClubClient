package com.cobbleclub.client.pokemonpreview;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeatureAssignments;
import com.cobblemon.mod.common.client.render.ModelAssetVariation;
import com.cobblemon.mod.common.client.render.VaryingRenderableResolver;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class PokemonFormCatalog {
    public static final PokemonFormCatalog INSTANCE = new PokemonFormCatalog();

    private static final Set<String> DEFAULT_FORM_ASPECTS = linkedSet(
        "galarian", "hisuian", "alolan", "paldean", "mega", "mega-x", "mega-y", "mega-z",
        "primal", "crowned", "origin-forme", "origin", "unbound", "therian", "complete",
        "white", "black", "ice", "shadow", "dusk-mane", "dawn-wings", "ultra",
        "wellspring", "hearthflame", "cornerstone"
    );

    private static final Set<String> DEFAULT_FORMS_GLOBAL = linkedSet(
        "mega", "mega-x", "mega-y", "mega-z", "mega_x", "mega_y", "mega_z", "primal",
        "crowned", "unbound", "therian", "dusk-mane", "dawn-wings", "origin", "origin-forme",
        "wellspring", "hearthflame", "cornerstone", "resolute", "rapidstrike", "bloodmoon",
        "hero-form", "school-form", "three-segment-form", "meteor-form", "core-form",
        "noice_face", "hangry-mode", "zen-mode", "zen_mode", "complete-percent",
        "heat-appliance", "wash-appliance", "frost-appliance", "fan-appliance", "mow-appliance"
    );

    private static final Map<String, Set<String>> DEFAULT_FORMS_SCOPED = defaultScopedForms();
    private static Set<String> formAspects = DEFAULT_FORM_ASPECTS;
    private static Set<String> formsGlobal = DEFAULT_FORMS_GLOBAL;
    private static Map<String, Set<String>> formsScoped = DEFAULT_FORMS_SCOPED;
    private static final Set<String> CHAIN_SUPPRESSED = Set.of("ultranecrozma");

    private static final List<Candidate> EXTRA_CANDIDATES = List.of(
        new Candidate("giratina", List.of("origin-forme"), "Giratina Origin", false),
        new Candidate("dialga", List.of("origin-forme"), "Dialga Origin", false),
        new Candidate("palkia", List.of("origin-forme"), "Palkia Origin", false),
        new Candidate("ogerpon", List.of("wellspring"), "Ogerpon Wellspring", false),
        new Candidate("ogerpon", List.of("hearthflame"), "Ogerpon Hearthflame", false),
        new Candidate("ogerpon", List.of("cornerstone"), "Ogerpon Cornerstone", false),
        new Candidate("zygarde", List.of("complete", "mega"), "Zygarde Mega", false),
        new Candidate("magearna", List.of("origin", "mega"), "Magearna Mega", false),
        new Candidate("ashgreninja", List.of(), "Ash Greninja", true),
        new Candidate("ashpikachu", List.of(), "Ash Pikachu", true)
    );

    private PokemonFormCatalog() {
    }

    private static Set<String> linkedSet(String... values) {
        return Set.copyOf(new LinkedHashSet<>(List.of(values)));
    }

    private static Map<String, Set<String>> defaultScopedForms() {
        Map<String, Set<String>> scoped = new LinkedHashMap<>();
        scoped.put("kyurem", Set.of("white", "black"));
        scoped.put("calyrex", Set.of("ice", "shadow"));
        scoped.put("zygarde", Set.of("complete"));
        scoped.put("necrozma", Set.of("ultra"));
        scoped.put("lycanroc", Set.of("dusk", "midnight"));
        scoped.put("rockruff", Set.of("dusk", "midnight"));
        scoped.put("castform", Set.of("sunny", "rainy", "snowy"));
        return Map.copyOf(scoped);
    }

    public static void configure(
        @Nullable List<String> aspects,
        @Nullable List<String> buttons,
        @Nullable Map<String, ? extends List<String>> scoped
    ) {
        formAspects = aspects != null && !aspects.isEmpty()
            ? new LinkedHashSet<>(aspects)
            : DEFAULT_FORM_ASPECTS;
        formsGlobal = buttons != null && !buttons.isEmpty()
            ? new LinkedHashSet<>(buttons)
            : DEFAULT_FORMS_GLOBAL;

        if (scoped != null && !scoped.isEmpty()) {
            Map<String, Set<String>> converted = new LinkedHashMap<>();
            scoped.forEach((species, values) -> converted.put(species, new LinkedHashSet<>(values)));
            formsScoped = converted;
        } else {
            formsScoped = DEFAULT_FORMS_SCOPED;
        }
    }

    public static boolean isAlternateBase(@NotNull String aspect) {
        return formAspects.contains(aspect);
    }

    private boolean isFormAspect(String speciesPath, String aspect) {
        return formAspects.contains(aspect)
            || formsGlobal.contains(aspect)
            || formsScoped.getOrDefault(speciesPath, Set.of()).contains(aspect);
    }

    @NotNull
    public static List<FormEntry> skinFamilyEntries(
        @NotNull String skinAspect,
        @NotNull Set<String> prizeSpecies
    ) {
        List<DexEntry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Species species : PokemonSpecies.getSpecies()) {
            String path = species.getResourceIdentifier().getPath();
            boolean assigned = SpeciesFeatureAssignments.getFeatures(species).contains(skinAspect);
            if (!assigned && !prizeSpecies.contains(path)) {
                continue;
            }

            List<ModelAssetVariation> variations = INSTANCE.variationsOf(species.getResourceIdentifier());
            for (ModelAssetVariation variation : variations) {
                Set<String> variationAspects = variation.getAspects();
                if (variationAspects.contains("shiny") || !variationAspects.contains(skinAspect)) {
                    continue;
                }

                Set<String> extras = new LinkedHashSet<>(variationAspects);
                extras.remove(skinAspect);

                List<String> formExtras = extras.stream()
                    .filter(value -> INSTANCE.isFormAspect(path, value))
                    .sorted()
                    .collect(Collectors.toCollection(ArrayList::new));

                Set<String> appearanceAspects = new LinkedHashSet<>(formExtras);
                appearanceAspects.add(skinAspect);
                if (INSTANCE.appearanceKey(species.getResourceIdentifier(), appearanceAspects) == null) {
                    formExtras = extras.stream().sorted().collect(Collectors.toCollection(ArrayList::new));
                }

                String seenKey = path + "|" + String.join(",", formExtras);
                if (!seen.add(seenKey)) {
                    continue;
                }

                String name = species.getTranslatedName().getString();
                String label = formExtras.isEmpty()
                    ? name
                    : formExtras.stream().map(INSTANCE::prettify).collect(Collectors.joining(" ")) + " " + name;
                List<String> entryAspects = new ArrayList<>();
                entryAspects.add(skinAspect);
                entryAspects.addAll(formExtras);
                entries.add(new DexEntry(
                    species.getNationalPokedexNumber(),
                    new FormEntry(species.getResourceIdentifier().toString(), entryAspects, label)
                ));
            }

            boolean hasSkinVariation = variations.stream()
                .anyMatch(variation -> variation.getAspects().contains(skinAspect));
            if (assigned && !hasSkinVariation && seen.add(path + "|")) {
                entries.add(new DexEntry(
                    species.getNationalPokedexNumber(),
                    new FormEntry(
                        species.getResourceIdentifier().toString(),
                        List.of(skinAspect),
                        species.getTranslatedName().getString()
                    )
                ));
            }
        }

        entries.sort(Comparator.comparingInt(DexEntry::dex));
        return entries.stream().map(DexEntry::entry).collect(Collectors.toCollection(ArrayList::new));
    }

    @NotNull
    public static List<FormEntry> prizeFormButtons(
        @NotNull String species,
        @NotNull List<String> aspects,
        @Nullable List<String> evolutions
    ) {
        Identifier prizeId = Identifier.tryParse(species);
        Species prizeSpecies = prizeId == null ? null : PokemonSpecies.getByIdentifier(prizeId);
        String prizePath = prizeSpecies == null ? null : prizeSpecies.getResourceIdentifier().getPath();

        List<String> carried = aspects.stream()
            .filter(value -> value != null)
            .filter(value -> !value.equals("shiny"))
            .filter(value -> !formsGlobal.contains(value))
            .filter(value -> !formsScoped.getOrDefault(prizePath, Set.of()).contains(value))
            .collect(Collectors.toCollection(ArrayList::new));

        List<String> skin = carried.stream()
            .filter(value -> INSTANCE.isRealAspect(prizeSpecies, value))
            .collect(Collectors.toCollection(ArrayList::new));
        Set<String> skinSet = new LinkedHashSet<>(skin);
        List<String> inert = carried.stream()
            .filter(value -> !skinSet.contains(value))
            .collect(Collectors.toCollection(ArrayList::new));
        List<String> shiny = aspects.contains("shiny") ? List.of("shiny") : List.of();
        List<String> skinPrefix = skin.stream().filter(formAspects::contains).sorted().toList();

        List<String> chain = new ArrayList<>();
        chain.add(species);
        if (evolutions != null) {
            for (String evolution : evolutions) {
                if (evolution != null && !chain.contains(evolution)) {
                    chain.add(evolution);
                }
            }
        }

        List<FormEntry> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < chain.size(); index++) {
            String idString = chain.get(index);
            Identifier id = Identifier.tryParse(idString);
            if (id == null || (index > 0 && CHAIN_SUPPRESSED.contains(id.getPath()))) {
                continue;
            }

            Species chainSpecies = PokemonSpecies.getByIdentifier(id);
            if (chainSpecies == null) {
                continue;
            }

            String name = chainSpecies.getTranslatedName().getString();
            List<ModelAssetVariation> variations = INSTANCE.variationsOf(id);

            if (index > 0) {
                boolean hasSkinArt = skin.isEmpty() || variations.stream().anyMatch(variation ->
                    !variation.getAspects().contains("shiny")
                        && !variation.getAspects().isEmpty()
                        && skin.containsAll(variation.getAspects())
                );

                String label = labelWithPrefix(skinPrefix, name);
                if (hasSkinArt && seen.add(idString + "|" + skin)) {
                    List<String> entryAspects = combined(skin, inert, shiny);
                    out.add(new FormEntry(idString, entryAspects, label));
                }
            }

            List<FormEntry> formButtons = new ArrayList<>();
            for (ModelAssetVariation variation : variations) {
                Set<String> variationAspects = variation.getAspects();
                if (variationAspects.contains("shiny") || !variationAspects.containsAll(skin)) {
                    continue;
                }

                List<String> extras = variationAspects.stream()
                    .filter(value -> !skinSet.contains(value))
                    .sorted()
                    .collect(Collectors.toCollection(ArrayList::new));
                if (extras.isEmpty()) {
                    continue;
                }

                Set<String> scoped = formsScoped.getOrDefault(id.getPath(), Set.of());
                boolean allForms = extras.stream().allMatch(value -> formsGlobal.contains(value) || scoped.contains(value));
                if (!allForms) {
                    continue;
                }

                List<String> full = new ArrayList<>(skin);
                full.addAll(extras);
                if (!seen.add(idString + "|" + full)) {
                    continue;
                }

                List<String> labelAspects = new ArrayList<>(extras);
                labelAspects.addAll(skinPrefix);
                String label = labelWithPrefix(labelAspects, name);
                formButtons.add(new FormEntry(idString, combined(full, inert, shiny), label));
            }

            formButtons.sort(Comparator.comparing(FormEntry::getLabel));
            out.addAll(formButtons);
        }

        return out;
    }

    private static List<String> combined(List<String> first, List<String> second, List<String> third) {
        List<String> result = new ArrayList<>(first.size() + second.size() + third.size());
        result.addAll(first);
        result.addAll(second);
        result.addAll(third);
        return result;
    }

    private static String labelWithPrefix(List<String> aspects, String name) {
        if (aspects.isEmpty()) {
            return name;
        }
        return aspects.stream().map(INSTANCE::prettify).collect(Collectors.joining(" ")) + " " + name;
    }

    private boolean isRealAspect(Species species, String aspect) {
        if (species == null) {
            return true;
        }
        if (SpeciesFeatureAssignments.getFeatures(species).contains(aspect)) {
            return true;
        }
        if (species.getForms().stream().anyMatch(form -> form.getAspects().contains(aspect))) {
            return true;
        }
        return variationsOf(species.getResourceIdentifier()).stream()
            .anyMatch(variation -> variation.getAspects().contains(aspect));
    }

    @NotNull
    public static List<FormFamily> build(@NotNull Set<String> excludeAspects) {
        HashMap<String, Kept> byAppearance = new HashMap<>();

        for (Species species : PokemonSpecies.getSpecies()) {
            String speciesName = species.getTranslatedName().getString();

            for (String aspect : SpeciesFeatureAssignments.getFeatures(species)) {
                if (!excludeAspects.contains(aspect)) {
                    consider(
                        byAppearance,
                        species,
                        List.of(aspect),
                        aspect.toLowerCase(Locale.ROOT),
                        speciesName + " " + INSTANCE.prettify(aspect)
                    );
                }
            }

            for (FormData form : species.getForms()) {
                if (form == species.getStandardForm() || form.getAspects().isEmpty()) {
                    continue;
                }
                List<String> formValues = new ArrayList<>(form.getAspects());
                if (formValues.stream().anyMatch(excludeAspects::contains)) {
                    continue;
                }
                String hint = (form.getName() + " " + String.join(" ", formValues)).toLowerCase(Locale.ROOT);
                consider(byAppearance, species, formValues, hint, speciesName + " " + form.getName());
            }
        }

        for (Candidate candidate : EXTRA_CANDIDATES) {
            Identifier id = Identifier.of("cobblemon", candidate.species);
            if (PokemonSpecies.getByIdentifier(id) == null) {
                continue;
            }
            String appearance = candidate.standalone
                ? "standalone"
                : INSTANCE.appearanceKey(id, new LinkedHashSet<>(candidate.aspects));
            if (appearance != null) {
                keep(
                    byAppearance,
                    candidate.species,
                    "forms",
                    appearance,
                    new FormEntry(id.toString(), candidate.aspects, candidate.label)
                );
            }
        }

        List<FormEntry> forms = new ArrayList<>();
        List<FormEntry> regional = new ArrayList<>();
        for (Kept kept : byAppearance.values()) {
            (kept.bucket.equals("forms") ? forms : regional).add(kept.entry);
        }

        Comparator<FormEntry> byLabel = Comparator.comparing(FormEntry::getLabel);
        List<FormFamily> families = new ArrayList<>();
        if (!forms.isEmpty()) {
            forms.sort(byLabel);
            families.add(new FormFamily("Forms", forms));
        }
        if (!regional.isEmpty()) {
            regional.sort(byLabel);
            families.add(new FormFamily("Regional", regional));
        }
        return families;
    }

    @Nullable
    private String appearanceKey(Identifier species, Set<String> aspects) {
        List<ModelAssetVariation> variations = variationsOf(species);
        List<String> applied = new ArrayList<>();
        for (int index = 0; index < variations.size(); index++) {
            Set<String> variationAspects = variations.get(index).getAspects();
            if (!variationAspects.contains("shiny")
                && !variationAspects.isEmpty()
                && aspects.containsAll(variationAspects)) {
                applied.add(Integer.toString(index));
            }
        }
        return applied.isEmpty() ? null : String.join(",", applied);
    }

    private List<ModelAssetVariation> variationsOf(Identifier species) {
        VaryingRenderableResolver resolver = VaryingModelRepository.INSTANCE.getVariations().get(species);
        return resolver == null ? List.of() : resolver.getVariations();
    }

    private String prettify(String aspect) {
        String[] parts = aspect.split("[-_]", -1);
        List<String> pretty = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (part.isEmpty()) {
                pretty.add(part);
            } else {
                pretty.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
            }
        }
        return String.join(" ", pretty);
    }

    private static void keep(
        HashMap<String, Kept> byAppearance,
        String path,
        String bucket,
        String appearance,
        FormEntry entry
    ) {
        String key = path + "|" + appearance;
        Kept existing = byAppearance.get(key);
        if (existing == null || entry.getAspects().size() < existing.aspectCount) {
            byAppearance.put(key, new Kept(bucket, entry.getAspects().size(), entry));
        }
    }

    private static void consider(
        HashMap<String, Kept> byAppearance,
        Species species,
        List<String> aspects,
        String hint,
        String label
    ) {
        String path = species.getResourceIdentifier().getPath();
        boolean isConfiguredForm = aspects.stream().anyMatch(value ->
            formsGlobal.contains(value) || formsScoped.getOrDefault(path, Set.of()).contains(value)
        );

        String bucket;
        if (isConfiguredForm) {
            bucket = "forms";
        } else if (hint.contains("alolan")
            || hint.contains("galarian")
            || hint.contains("hisuian")
            || hint.contains("paldean")) {
            bucket = "regional";
        } else {
            return;
        }

        String appearance = INSTANCE.appearanceKey(species.getResourceIdentifier(), new LinkedHashSet<>(aspects));
        if (appearance != null) {
            keep(
                byAppearance,
                path,
                bucket,
                appearance,
                new FormEntry(species.getResourceIdentifier().toString(), aspects, label)
            );
        }
    }

    private record DexEntry(int dex, FormEntry entry) {
    }

    @Environment(EnvType.CLIENT)
    public static final class FormEntry {
        private final String species;
        private final List<String> aspects;
        private final String label;

        public FormEntry(@NotNull String species, @NotNull List<String> aspects, @NotNull String label) {
            this.species = species;
            this.aspects = aspects;
            this.label = label;
        }

        @NotNull
        public String getSpecies() {
            return species;
        }

        @NotNull
        public List<String> getAspects() {
            return aspects;
        }

        @NotNull
        public String getLabel() {
            return label;
        }
    }

    @Environment(EnvType.CLIENT)
    public static final class FormFamily {
        private final String label;
        private final List<FormEntry> entries;

        public FormFamily(@NotNull String label, @NotNull List<FormEntry> entries) {
            this.label = label;
            this.entries = entries;
        }

        @NotNull
        public String getLabel() {
            return label;
        }

        @NotNull
        public List<FormEntry> getEntries() {
            return entries;
        }
    }

    private record Candidate(String species, List<String> aspects, String label, boolean standalone) {
    }

    private record Kept(String bucket, int aspectCount, FormEntry entry) {
    }
}
