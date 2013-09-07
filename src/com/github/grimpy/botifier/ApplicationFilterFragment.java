package com.github.grimpy.botifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;

public class ApplicationFilterFragment extends PreferenceFragment {
	private SharedPreferences mSharedPref;
	private PreferenceCategory mBlackList;
	private Set<String> mBlackListEntries;
	private PackageManager mPackageManager;
	private ColorFilter mGrayscaleFilter;
	
	class AppPreference extends CheckBoxPreference  {
		private String mPkgName; 

		public String getPkgName() {
			return mPkgName;
		}

		public void setPkgName(String mPkgName) {
			this.mPkgName = mPkgName;
		}

		public AppPreference(Context context) {
			super(context);
		}
		
	}
	class ApplicationComparator implements Comparator<AppPreference> {
	    @Override
	    public int compare(AppPreference a, AppPreference b) {
	        return a.getTitle().toString().compareTo(b.getTitle().toString());
	    }
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mPackageManager = getActivity().getPackageManager();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0f);
        float[] matrix = colorMatrix.getArray();
        matrix[18] = 0.5f;
        mGrayscaleFilter = new ColorMatrixColorFilter(colorMatrix);
		addPreferencesFromResource(R.xml.list_preference);
		mBlackList = (PreferenceCategory) findPreference(getString(R.string.cat_filterlist));
		mBlackList.setTitle(R.string.applications);
		Set<String> entries = mSharedPref.getStringSet(getString(R.string.pref_blocked_applist), null);
		if (entries == null) {
			mBlackListEntries = new HashSet<String>();
		} else {
			mBlackListEntries = new HashSet<String>(entries);
		}
		List<ApplicationInfo> pkgs = mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA);
		List<AppPreference> prefs = new ArrayList<ApplicationFilterFragment.AppPreference>();
		
		for (ApplicationInfo pkg: pkgs) {
			AppPreference pref = new AppPreference(getActivity());
			pref.setTitle(mPackageManager.getApplicationLabel(pkg));
			Drawable icon = pkg.loadIcon(mPackageManager);
			pref.setPkgName(pkg.packageName);
			if (mBlackListEntries.contains(pkg.packageName)) {
				pref.setDefaultValue(false);
                icon.setColorFilter(mGrayscaleFilter);
            } else {
				pref.setDefaultValue(true);
			}
			pref.setIcon(icon);
			prefs.add(pref);
		}
		Collections.sort(prefs, new ApplicationComparator());
		for (AppPreference pref: prefs) {
			mBlackList.addPreference(pref);
		}
	}
	
	private void editEntry(String pkg, boolean enabled) {
		ArrayList<String> newlist = new ArrayList<String>(mBlackListEntries);
		boolean isblacklisted = newlist.contains(pkg);
		if (enabled && isblacklisted) {
			return;
		} else if (enabled) {
			newlist.remove(pkg);
		} else if (!enabled && !isblacklisted){
			return;
		} else if (!enabled) {
			newlist.add(pkg);
		}
		mBlackListEntries = new HashSet<String>(newlist);
		Editor editor = mSharedPref.edit();
        editor.putStringSet(getString(R.string.pref_blocked_applist), mBlackListEntries);
        editor.apply();
	}
	
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, final Preference preference) {
		AppPreference pref = (AppPreference) preference;
		editEntry(pref.getPkgName(), pref.isChecked());
		Drawable icon = pref.getIcon();
		if (!pref.isChecked()) {
			icon.setColorFilter(null);
		} else {
			icon.setColorFilter(mGrayscaleFilter);
		}
		return true;
	}    
}
