package ru.solrudev.ackpine.sample.install;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.core.view.ViewKt;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import ru.solrudev.ackpine.sample.R;
import ru.solrudev.ackpine.sample.databinding.ItemInstallSessionBinding;
import ru.solrudev.ackpine.session.Progress;
import ru.solrudev.ackpine.session.parameters.NotificationString;

public final class InstallSessionsAdapter extends ListAdapter<SessionData, InstallSessionsAdapter.SessionViewHolder> {

    private final static SessionDiffCallback DIFF_CALLBACK = new SessionDiffCallback();
    private final Consumer<UUID> _onClick;

    public InstallSessionsAdapter(Consumer<UUID> onClick) {
        super(DIFF_CALLBACK);
        _onClick = onClick;
    }

    public static class SessionViewHolder extends RecyclerView.ViewHolder {

        private final ItemInstallSessionBinding _binding;
        private final Consumer<UUID> _onClick;
        private SessionData _currentSessionData;

        public SessionViewHolder(@NonNull View itemView, Consumer<UUID> onClick) {
            super(itemView);
            _binding = ItemInstallSessionBinding.bind(itemView);
            _onClick = onClick;
            _binding.sessionButton.setOnClickListener(v -> _onClick.accept(_currentSessionData.id()));
        }

        public boolean isSwipeable() {
            return !_currentSessionData.error().isEmpty();
        }

        @NonNull
        public UUID getSessionId() {
            return _currentSessionData.id();
        }

        public void bind(@NonNull SessionData sessionData) {
            _currentSessionData = sessionData;
            _binding.sessionName.setText(sessionData.name());
            setError(sessionData.error());
        }

        public void setProgress(@NonNull Progress sessionProgress) {
            final var progress = sessionProgress.getProgress();
            final var max = sessionProgress.getMax();
            _binding.sessionProgressBar.setProgressCompat(progress, true);
            _binding.sessionProgressBar.setMax(max);
            _binding.sessionPercentage.setText(itemView.getContext().getString(
                    R.string.percentage, (int) (((double) progress) / max * 100)));
        }

        private void setError(@NonNull NotificationString error) {
            final var hasError = !error.isEmpty();
            ViewKt.setVisible(_binding.sessionName, !hasError);
            ViewKt.setVisible(_binding.sessionProgressBar, !hasError);
            ViewKt.setVisible(_binding.sessionPercentage, !hasError);
            ViewKt.setVisible(_binding.sessionButton, !hasError);
            ViewKt.setVisible(_binding.sessionError, hasError);
            _binding.sessionError.setText(error.resolve(itemView.getContext()));
        }
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final var view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_install_session, parent, false);
        return new SessionViewHolder(view, _onClick);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        onBindViewHolder(holder, position, Collections.emptyList());
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position, @NonNull List<Object> payloads) {
        final var sessionData = getItem(position);
        if (payloads.isEmpty()) {
            holder.bind(sessionData);
        } else {
            holder.setProgress((Progress) payloads.get(0));
        }
    }

    public void submitProgress(@NonNull List<SessionProgress> progress) {
        for (int i = 0; i < progress.size(); i++) {
            notifyItemChanged(i, progress.get(i).progress());
        }
    }

    private static class SessionDiffCallback extends DiffUtil.ItemCallback<SessionData> {

        @Override
        public boolean areItemsTheSame(@NonNull SessionData oldItem, @NonNull SessionData newItem) {
            return oldItem.id().equals(newItem.id());
        }

        @Override
        public boolean areContentsTheSame(@NonNull SessionData oldItem, @NonNull SessionData newItem) {
            return oldItem.equals(newItem);
        }
    }
}