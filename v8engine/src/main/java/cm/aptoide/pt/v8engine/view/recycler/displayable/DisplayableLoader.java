/*
 * Copyright (c) 2016.
 * Modified by Neurophobic Animal on 04/05/2016.
 */

package cm.aptoide.pt.v8engine.view.recycler.displayable;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.model.v7.store.GetStoreWidgets;
import cm.aptoide.pt.utils.MultiDexHelper;
import cm.aptoide.pt.v8engine.Aptoide;
import dalvik.system.DexFile;

/**
 * @author sithengineer
 */
public enum DisplayableLoader {
	INSTANCE;

	private static final String TAG = DisplayableLoader.class.getName();

	//TODO use a eager loading technique and remove synchronization primitives

	private HashMap<GetStoreWidgets.Type, Class<? extends Displayable>> displayableHashMap;
	private LruCache<GetStoreWidgets.Type, Class<? extends Displayable>> displayableLruCache;

	private synchronized void loadDisplayables() {
		displayableHashMap = new HashMap<>();
//		long nanos = System.
		try {
			// get the current class loader
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			// current package name for filtering purposes
			String packageName = getClass().getPackage().getName();

			List<Map.Entry<String, DexFile>> classNames =
					MultiDexHelper.getAllClasses(Aptoide.getContext());

			for(Map.Entry<String, DexFile> className : classNames ) {

				// if the class doesn't belong in the current project we discard it
				// useful for speeding this method
				if (!className.getKey().startsWith(packageName)) continue;
				Class<?> displayableClass = className.getValue().loadClass(
						className.getKey(), classLoader);

				if (displayableClass != null && Displayable.class.isAssignableFrom(displayableClass)) {
					try {
						Displayable d = (Displayable) displayableClass.newInstance();
						displayableHashMap.put(d.getType(),
								(Class<? extends Displayable>) displayableClass
						);
					} catch (Exception e) {
						Log.e(TAG, "", e);
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}

//		nanos -= System.nanoTime();
//		nanos *= -1;
//		Log.v(TAG, String.format("loadWidgets() took %d millis", nanos / 1000000));

		if (displayableHashMap.size() == 0) {
			throw new IllegalStateException("Unable to load Displayables");
		}
		int cacheSize = displayableHashMap.size() / 4;
		displayableLruCache = new LruCache<>(cacheSize == 0 ? 2 : cacheSize); // a quarter of the total, or 2
	}

	@Nullable
	public synchronized Displayable newDisplayable(@NonNull GetStoreWidgets.Type type) {
		if (displayableHashMap == null) {
			loadDisplayables();
		}

		Class<? extends Displayable> displayableClass = displayableLruCache.get(type);

		if (displayableClass == null) {

			displayableClass = displayableHashMap.get(type);
			displayableLruCache.put(type, displayableClass);
		}

		try {
			return displayableClass.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Nullable
	public synchronized <T> DisplayablePojo<T> newDisplayable(@NonNull GetStoreWidgets.Type type, T pojo) {
		Displayable displayable = newDisplayable(type);

		if (displayable != null && displayable instanceof DisplayablePojo<?>) {
			try {
				return ((DisplayablePojo<T>) displayable).setPojo(pojo);
			} catch (ClassCastException e) {
				Logger.e(TAG, "Trying to instantiate a DisplayablePojo with a wrong type!");
			}
		} else {
			Logger.e(TAG, "Trying to instantiate a standard Displayable with a pojo!");
		}

		return null;
	}

}
