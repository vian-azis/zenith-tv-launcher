package com.zenith.launcher.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.zenith.launcher.R;
import com.zenith.launcher.model.MovieItem;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MovieCatalog {

    private static final String TAG = "MovieCatalog";
    private static final String CACHE_FILE = "movies_cache.json";
    private static final String TMDB_API_KEY = "4e44d9029b1270a757cddc766a1bcb63";
    private static final String TMDB_TRENDING_URL = "https://api.themoviedb.org/3/trending/all/day?api_key=" + TMDB_API_KEY;
    private static final String TMDB_POPULAR_URL = "https://api.themoviedb.org/3/movie/popular?api_key=" + TMDB_API_KEY;

    private static final List<MovieItem> MOVIES = Collections.synchronizedList(new ArrayList<>());
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final Map<Integer, String> GENRES_MAP = new HashMap<>();

    static {
        GENRES_MAP.put(28, "Action");
        GENRES_MAP.put(12, "Adventure");
        GENRES_MAP.put(16, "Animation");
        GENRES_MAP.put(35, "Comedy");
        GENRES_MAP.put(80, "Crime");
        GENRES_MAP.put(99, "Documentary");
        GENRES_MAP.put(18, "Drama");
        GENRES_MAP.put(10751, "Family");
        GENRES_MAP.put(14, "Fantasy");
        GENRES_MAP.put(36, "History");
        GENRES_MAP.put(27, "Horror");
        GENRES_MAP.put(10402, "Music");
        GENRES_MAP.put(9648, "Mystery");
        GENRES_MAP.put(10749, "Romance");
        GENRES_MAP.put(878, "Sci-Fi");
        GENRES_MAP.put(10770, "TV Movie");
        GENRES_MAP.put(53, "Thriller");
        GENRES_MAP.put(10752, "War");
        GENRES_MAP.put(37, "Western");
        GENRES_MAP.put(10759, "Action & Adventure");
        GENRES_MAP.put(10765, "Sci-Fi & Fantasy");

        initDefaults();
    }

    private static void initDefaults() {
        MOVIES.clear();
        MOVIES.add(new MovieItem(
            "Peaky Blinders",
            "2022",
            "8.8",
            "6 Musim",
            "Crime • Drama",
            "4K ULTRA HD • HDR",
            "Faced with devastating news in Birmingham, Tommy Shelby goes on a quest to discover who it was that placed a curse on his family.",
            "https://image.tmdb.org/t/p/w1280/dzq83RHwQcnP6WGJ6YkenIqeaa5.jpg",
            R.drawable.bg_wallpaper_peaky,
            "com.netflix.ninja",
            "Peaky Blinders"
        ));

        MOVIES.add(new MovieItem(
            "Joker ( 2019 )",
            "2019",
            "8.4",
            "2j 2m",
            "Crime • Drama • Thriller",
            "4K ULTRA HD • DOLBY VISION",
            "The film, based on DC Comics characters, stars Joaquin Phoenix as the Joker navigating life in a decaying Gotham City.",
            "https://image.tmdb.org/t/p/w1280/rlay2M5QYvi6igbGcFjq8jxeusY.jpg",
            R.drawable.bg_wallpaper_joker,
            "com.amazon.amazonvideo.livingroom",
            "Joker 2019"
        ));

        MOVIES.add(new MovieItem(
            "Oppenheimer ( 2023 )",
            "2023",
            "8.9",
            "3j 0m",
            "Biography • Drama • History",
            "4K IMAX • DOLBY ATMOS",
            "Kisah fisikawan J. Robert Oppenheimer yang memimpin Proyek Manhattan rahasia untuk mengembangkan senjata atom pertama.",
            "https://image.tmdb.org/t/p/w1280/neeNHeXjMF5fXoCJRsOmkNGC7q.jpg",
            R.drawable.bg_wallpaper_oppenheimer,
            "com.netflix.ninja",
            "Oppenheimer 2023"
        ));

        MOVIES.add(new MovieItem(
            "Avatar: The Way of Water",
            "2022",
            "7.6",
            "3j 12m",
            "Action • Adventure • Sci-Fi",
            "4K HDR10+ • 3D HFR",
            "Jake Sully dan Neytiri harus melindungi keluarga mereka dari ancaman manusia yang kembali ke samudra Pandora.",
            "https://image.tmdb.org/t/p/w1280/s16H6tpK2utvwDtzZ8Qy4qm5Emw.jpg",
            R.drawable.bg_wallpaper_avatar,
            "com.netflix.ninja",
            "Avatar The Way of Water"
        ));

        MOVIES.add(new MovieItem(
            "Interstellar ( 2014 )",
            "2014",
            "8.7",
            "2j 49m",
            "Adventure • Drama • Sci-Fi",
            "4K ULTRA HD • LOSSLESS",
            "Sekelompok astronot pemberani melintasi lubang cacing di dekat Saturnus guna mencari tempat tinggal baru bagi umat manusia.",
            "https://image.tmdb.org/t/p/w1280/8sNiAPPYU14PUepFNeSNGUTiHW.jpg",
            R.drawable.bg_wallpaper_interstellar,
            "com.netflix.ninja",
            "Interstellar"
        ));

        MOVIES.add(new MovieItem(
            "Dune: Part Two",
            "2024",
            "8.5",
            "2j 46m",
            "Action • Adventure • Sci-Fi",
            "4K IMAX • DOLBY VISION",
            "Paul Atreides bersatu dengan Chani dan Fremen untuk membalas dendam terhadap para konspirator yang menghancurkan keluarganya.",
            "https://image.tmdb.org/t/p/w1280/eZ239CUp1d6OryZEBPnO2n87gMG.jpg",
            R.drawable.bg_wallpaper_avatar,
            "com.netflix.ninja",
            "Dune Part Two"
        ));
    }

    public interface OnCatalogUpdatedListener {
        void onCatalogUpdated(List<MovieItem> updatedList);
    }

    public static synchronized List<MovieItem> getAllMovies() {
        return new ArrayList<>(MOVIES);
    }

    public static synchronized MovieItem getMovieForCurrentHour() {
        if (MOVIES.isEmpty()) initDefaults();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int index = hour % MOVIES.size();
        return MOVIES.get(index);
    }

    public static synchronized int getIndexOf(MovieItem item) {
        if (item == null) return 0;
        for (int i = 0; i < MOVIES.size(); i++) {
            if (MOVIES.get(i).getTitle().equalsIgnoreCase(item.getTitle())) {
                return i;
            }
        }
        return 0;
    }

    public static synchronized MovieItem getMovie(int index) {
        if (MOVIES.isEmpty()) initDefaults();
        if (index < 0 || index >= MOVIES.size()) return MOVIES.get(0);
        return MOVIES.get(index);
    }

    public static void initializeWithCache(Context context) {
        try {
            File file = new File(context.getFilesDir(), CACHE_FILE);
            if (file.exists() && file.length() > 50) {
                FileInputStream fis = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONArray arr = new JSONArray(sb.toString());
                if (arr.length() > 0) {
                    List<MovieItem> cached = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        MovieItem m = MovieItem.fromJson(arr.getJSONObject(i));
                        if (m != null && m.getBackdropUrl() != null && !m.getBackdropUrl().isEmpty()) {
                            cached.add(m);
                        }
                    }
                    if (!cached.isEmpty()) {
                        synchronized (MOVIES) {
                            MOVIES.clear();
                            MOVIES.addAll(cached);
                        }
                        Log.d(TAG, "Loaded " + cached.size() + " movies from offline cache");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading cache: " + e.getMessage());
        }
    }

    public static void fetchOnlineCatalog(Context context, OnCatalogUpdatedListener listener) {
        executor.execute(() -> {
            List<MovieItem> fetched = queryTmdb(TMDB_TRENDING_URL);
            if (fetched == null || fetched.isEmpty()) {
                fetched = queryTmdb(TMDB_POPULAR_URL);
            }

            if (fetched != null && !fetched.isEmpty()) {
                synchronized (MOVIES) {
                    MOVIES.clear();
                    MOVIES.addAll(fetched);
                }

                // Save to local cache file for offline resilience
                saveToCache(context, fetched);

                // Preload top backdrops into Glide cache for instant zero-lag switching
                try {
                    for (int i = 0; i < Math.min(6, fetched.size()); i++) {
                        String url = fetched.get(i).getBackdropUrl();
                        if (url != null && !url.isEmpty()) {
                            Glide.with(context.getApplicationContext())
                                .downloadOnly()
                                .load(url)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .submit();
                        }
                    }
                } catch (Exception ignored) {}

                final List<MovieItem> resultList = new ArrayList<>(fetched);
                if (listener != null) {
                    mainHandler.post(() -> listener.onCatalogUpdated(resultList));
                }
            }
        });
    }

    private static List<MovieItem> queryTmdb(String requestUrl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(requestUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject root = new JSONObject(sb.toString());
                JSONArray results = root.optJSONArray("results");
                if (results != null && results.length() > 0) {
                    List<MovieItem> list = new ArrayList<>();
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject m = results.getJSONObject(i);
                        String backdropPath = m.optString("backdrop_path", "");
                        if (backdropPath.isEmpty() || backdropPath.equals("null")) {
                            backdropPath = m.optString("poster_path", "");
                        }
                        if (backdropPath.isEmpty() || backdropPath.equals("null")) {
                            continue;
                        }

                        String title = m.optString("title", "");
                        if (title.isEmpty()) {
                            title = m.optString("name", "Untitled");
                        }

                        String releaseDate = m.optString("release_date", "");
                        if (releaseDate.isEmpty()) {
                            releaseDate = m.optString("first_air_date", "2024");
                        }
                        String year = releaseDate.length() >= 4 ? releaseDate.substring(0, 4) : "2024";

                        double voteAvg = m.optDouble("vote_average", 8.0);
                        String rating = String.format(java.util.Locale.US, "%.1f", voteAvg);

                        String overview = m.optString("overview", "Tayangan film & serial pilihan terbaik hari ini.");

                        // Extract genres
                        JSONArray genreIds = m.optJSONArray("genre_ids");
                        StringBuilder genreBuilder = new StringBuilder();
                        if (genreIds != null) {
                            for (int g = 0; g < Math.min(3, genreIds.length()); g++) {
                                int gid = genreIds.optInt(g);
                                String gname = GENRES_MAP.get(gid);
                                if (gname != null) {
                                    if (genreBuilder.length() > 0) genreBuilder.append(" • ");
                                    genreBuilder.append(gname);
                                }
                            }
                        }
                        String genres = genreBuilder.length() > 0 ? genreBuilder.toString() : "Drama • Cinema";

                        String fullBackdropUrl = "https://image.tmdb.org/t/p/w1280" + backdropPath;

                        list.add(new MovieItem(
                            title,
                            year,
                            rating,
                            "4K HDR",
                            genres,
                            "4K ULTRA HD • DOLBY VISION",
                            overview,
                            fullBackdropUrl,
                            R.drawable.bg_wallpaper_joker,
                            "com.netflix.ninja",
                            title
                        ));
                    }
                    return list;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "queryTmdb error: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    private static void saveToCache(Context context, List<MovieItem> items) {
        try {
            JSONArray arr = new JSONArray();
            for (MovieItem item : items) {
                arr.put(item.toJson());
            }
            File file = new File(context.getFilesDir(), CACHE_FILE);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(arr.toString().getBytes(StandardCharsets.UTF_8));
            fos.flush();
            fos.close();
            Log.d(TAG, "Cached " + items.size() + " movies to disk");
        } catch (Exception e) {
            Log.e(TAG, "saveToCache error: " + e.getMessage());
        }
    }
}
