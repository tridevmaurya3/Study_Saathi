package com.tridev.studysaathi.data.schooldirectory.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tridev.studysaathi.R;
import com.tridev.studysaathi.data.schooldirectory.entity
        .SchoolDirectoryEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class SchoolDirectorySchoolAdapter
        extends ArrayAdapter<SchoolDirectoryEntity> {

    private final LayoutInflater layoutInflater;

    @NonNull
    private final List<SchoolDirectoryEntity>
            completeSchoolList =
            new ArrayList<>();

    @NonNull
    private final List<SchoolDirectoryEntity>
            visibleSchoolList =
            new ArrayList<>();

    private final Filter schoolFilter =
            new Filter() {

                @Override
                protected FilterResults performFiltering(
                        @Nullable CharSequence constraint
                ) {
                    String searchText =
                            normalizeSearchText(
                                    constraint
                            );

                    List<SchoolDirectoryEntity>
                            filteredSchools =
                            new ArrayList<>();

                    if (searchText.isEmpty()) {
                        filteredSchools.addAll(
                                completeSchoolList
                        );

                    } else {
                        for (SchoolDirectoryEntity school :
                                completeSchoolList) {

                            if (matchesSearch(
                                    school,
                                    searchText
                            )) {
                                filteredSchools.add(
                                        school
                                );
                            }
                        }
                    }

                    FilterResults filterResults =
                            new FilterResults();

                    filterResults.values =
                            filteredSchools;

                    filterResults.count =
                            filteredSchools.size();

                    return filterResults;
                }

                @Override
                protected void publishResults(
                        @Nullable CharSequence constraint,
                        @NonNull FilterResults results
                ) {
                    visibleSchoolList.clear();

                    Object resultValues =
                            results.values;

                    if (resultValues
                            instanceof List<?>) {

                        for (Object item :
                                (List<?>) resultValues) {

                            if (item
                                    instanceof SchoolDirectoryEntity) {

                                visibleSchoolList.add(
                                        (SchoolDirectoryEntity) item
                                );
                            }
                        }
                    }

                    clear();

                    addAll(
                            visibleSchoolList
                    );

                    if (visibleSchoolList.isEmpty()) {
                        notifyDataSetInvalidated();

                    } else {
                        notifyDataSetChanged();
                    }
                }

                @Override
                public CharSequence convertResultToString(
                        @Nullable Object resultValue
                ) {
                    if (resultValue
                            instanceof SchoolDirectoryEntity) {

                        return ((SchoolDirectoryEntity)
                                resultValue)
                                .getSchoolName();
                    }

                    return "";
                }
            };

    public SchoolDirectorySchoolAdapter(
            @NonNull Context context
    ) {
        super(
                context,
                R.layout.item_school_directory_option,
                new ArrayList<>()
        );

        layoutInflater =
                LayoutInflater.from(
                        context
                );

        setNotifyOnChange(
                false
        );
    }

    public void submitSchools(
            @Nullable List<SchoolDirectoryEntity> schools
    ) {
        completeSchoolList.clear();
        visibleSchoolList.clear();

        if (schools != null) {
            completeSchoolList.addAll(
                    schools
            );

            visibleSchoolList.addAll(
                    schools
            );
        }

        clear();

        addAll(
                visibleSchoolList
        );

        notifyDataSetChanged();
    }

    public void clearSchools() {
        completeSchoolList.clear();
        visibleSchoolList.clear();

        clear();

        notifyDataSetChanged();
    }

    @NonNull
    public List<SchoolDirectoryEntity>
    getCurrentSchools() {
        return Collections.unmodifiableList(
                new ArrayList<>(
                        completeSchoolList
                )
        );
    }

    @Nullable
    public SchoolDirectoryEntity getSchoolAt(
            int position
    ) {
        if (position < 0
                || position >= getCount()) {

            return null;
        }

        return getItem(
                position
        );
    }

    public boolean hasSchools() {
        return !completeSchoolList.isEmpty();
    }

    @Override
    public int getCount() {
        return visibleSchoolList.size();
    }

    @Nullable
    @Override
    public SchoolDirectoryEntity getItem(
            int position
    ) {
        if (position < 0
                || position >= visibleSchoolList.size()) {

            return null;
        }

        return visibleSchoolList.get(
                position
        );
    }

    @NonNull
    @Override
    public View getView(
            int position,
            @Nullable View convertView,
            @NonNull ViewGroup parent
    ) {
        return createOrBindView(
                position,
                convertView,
                parent
        );
    }

    @NonNull
    @Override
    public View getDropDownView(
            int position,
            @Nullable View convertView,
            @NonNull ViewGroup parent
    ) {
        return createOrBindView(
                position,
                convertView,
                parent
        );
    }

    @NonNull
    private View createOrBindView(
            int position,
            @Nullable View convertView,
            @NonNull ViewGroup parent
    ) {
        SchoolOptionViewHolder viewHolder;

        if (convertView == null) {
            convertView =
                    layoutInflater.inflate(
                            R.layout
                                    .item_school_directory_option,
                            parent,
                            false
                    );

            viewHolder =
                    new SchoolOptionViewHolder(
                            convertView
                    );

            convertView.setTag(
                    viewHolder
            );

        } else {
            Object tag =
                    convertView.getTag();

            if (tag
                    instanceof SchoolOptionViewHolder) {

                viewHolder =
                        (SchoolOptionViewHolder)
                                tag;

            } else {
                viewHolder =
                        new SchoolOptionViewHolder(
                                convertView
                        );

                convertView.setTag(
                        viewHolder
                );
            }
        }

        SchoolDirectoryEntity school =
                getItem(
                        position
                );

        bindSchool(
                viewHolder,
                school
        );

        return convertView;
    }

    private void bindSchool(
            @NonNull SchoolOptionViewHolder viewHolder,
            @Nullable SchoolDirectoryEntity school
    ) {
        if (school == null) {
            viewHolder.schoolName.setText(
                    "School details unavailable"
            );

            viewHolder.schoolDetails.setText(
                    ""
            );

            viewHolder.verificationStatus
                    .setVisibility(
                            View.GONE
                    );

            return;
        }

        viewHolder.schoolName.setText(
                school.getSchoolName()
        );

        viewHolder.schoolDetails.setText(
                createSchoolDetails(
                        school
                )
        );

        if (school.isOfficiallyVerified()) {
            viewHolder.verificationStatus
                    .setText(
                            "Verified Directory Match"
                    );

            viewHolder.verificationStatus
                    .setVisibility(
                            View.VISIBLE
                    );

        } else {
            viewHolder.verificationStatus
                    .setText(
                            "Directory Record"
                    );

            viewHolder.verificationStatus
                    .setVisibility(
                            View.VISIBLE
                    );
        }
    }

    @NonNull
    private String createSchoolDetails(
            @NonNull SchoolDirectoryEntity school
    ) {
        StringBuilder detailsBuilder =
                new StringBuilder();

        appendDetail(
                detailsBuilder,
                formatBoardName(
                        school.getEducationBoard()
                )
        );

        appendDetail(
                detailsBuilder,
                school.getPreferredSchoolCode()
        );

        appendDetail(
                detailsBuilder,
                school.getAddressLine()
        );

        if (detailsBuilder.length() == 0) {
            return "School directory record";
        }

        return detailsBuilder.toString();
    }

    private void appendDetail(
            @NonNull StringBuilder builder,
            @Nullable Object value
    ) {
        String safeValue =
                safeText(
                        value
                );

        if (safeValue.isEmpty()) {
            return;
        }

        if (builder.length() > 0) {
            builder.append(
                    "  •  "
            );
        }

        builder.append(
                safeValue
        );
    }

    private boolean matchesSearch(
            @NonNull SchoolDirectoryEntity school,
            @NonNull String searchText
    ) {
        return normalizeSearchText(
                school.getSchoolName()
        ).contains(
                searchText
        )
                || normalizeSearchText(
                school.getSchoolNameHindi()
        ).contains(
                searchText
        )
                || normalizeSearchText(
                school.getUdiseCode()
        ).contains(
                searchText
        )
                || normalizeSearchText(
                school.getBoardAffiliationNumber()
        ).contains(
                searchText
        )
                || normalizeSearchText(
                school.getSchoolInternalCode()
        ).contains(
                searchText
        )
                || normalizeSearchText(
                school.getAddressLine()
        ).contains(
                searchText
        );
    }

    @NonNull
    private String normalizeSearchText(
            @Nullable Object value
    ) {
        return safeText(
                value
        )
                .toLowerCase(
                        Locale.ROOT
                )
                .replace(
                        "&",
                        " and "
                )
                .replaceAll(
                        "[^\\p{L}\\p{N}]+",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    @NonNull
    private String formatBoardName(
            @Nullable String board
    ) {
        String normalizedBoard =
                safeText(
                        board
                )
                        .toUpperCase(
                                Locale.ROOT
                        );

        switch (normalizedBoard) {
            case "CBSE":
                return "CBSE";

            case "CISCE":
                return "CISCE / ICSE / ISC";

            case "UPMSP":
                return "UP Board";

            case "STATE_BOARD":
                return "State Board";

            default:
                return normalizedBoard
                        .replace(
                                "_",
                                " "
                        );
        }
    }

    @NonNull
    private String safeText(
            @Nullable Object value
    ) {
        return value == null
                ? ""
                : value.toString()
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return schoolFilter;
    }

    private static final class
    SchoolOptionViewHolder {

        @NonNull
        private final TextView schoolName;

        @NonNull
        private final TextView schoolDetails;

        @NonNull
        private final TextView verificationStatus;

        private SchoolOptionViewHolder(
                @NonNull View itemView
        ) {
            schoolName =
                    itemView.findViewById(
                            R.id
                                    .textSchoolDirectoryOptionName
                    );

            schoolDetails =
                    itemView.findViewById(
                            R.id
                                    .textSchoolDirectoryOptionDetails
                    );

            verificationStatus =
                    itemView.findViewById(
                            R.id
                                    .textSchoolDirectoryOptionVerification
                    );
        }
    }
}