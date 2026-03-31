package com.example.waitwell.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.waitwell.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/** Karina's features:
 * Mostly backs user story 02.01.01 (Create Event & QR) from the organizer's perspective.
 * a little "completed!" confirmation screen that organizers see after creating an event.
 * This fragment only belongs to the Organizer module and never shows up for other roles.
 * It generates a QR code for the newly created event, lets the organizer share it out,
 * and offers a quick way back to the Organizer home list.

 */
public class OrganizerEventCreatedFragment extends Fragment {

    private static final String ARG_EVENT_ID = "event_id";
    private static final String ARG_EVENT_TITLE = "event_title";
    private static final String ARG_POSTER_URL = "event_poster_url";
    private static final String ARG_IS_PRIVATE = "is_private";
    private static final String ARG_IS_SHARE_MODE = "is_share_mode";

    // Values passed from the create event screen so this fragment knows
    // which event to show a QR code for and what text to include when sharing.
    private String eventId;
    private String eventTitle;
    private String eventPosterUrl;
    private boolean isPrivateEvent;
    private boolean isShareMode;

    private ImageView imgQrCode;

    /**
     * Simple factory method to package the fields we care about into arguments.
     * We keep the event id, title, and poster URL handy so the confirmation UI
     * can render a QR code and show useful share content.
     *
     * @param eventId    Firestore id for the newly created event
     * @param eventTitle title to show and include in share text
     * @param posterUrl  optional poster URL, kept mostly for consistency with the flow
     * @return a new {@link OrganizerEventCreatedFragment} with args set
     */
    public static OrganizerEventCreatedFragment newInstance(@NonNull String eventId,
                                                            @NonNull String eventTitle,
                                                            @Nullable String posterUrl,
                                                            boolean isPrivateEvent) {
        return newInstance(eventId, eventTitle, posterUrl, isPrivateEvent, false);
    }

    public static OrganizerEventCreatedFragment newInstance(@NonNull String eventId,
                                                            @NonNull String eventTitle,
                                                            @Nullable String posterUrl,
                                                            boolean isPrivateEvent,
                                                            boolean isShareMode) {
        OrganizerEventCreatedFragment fragment = new OrganizerEventCreatedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putString(ARG_EVENT_TITLE, eventTitle);
        args.putString(ARG_POSTER_URL, posterUrl);
        args.putBoolean(ARG_IS_PRIVATE, isPrivateEvent);
        args.putBoolean(ARG_IS_SHARE_MODE, isShareMode);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Fires the compact confirmation layout that shows the QR code,
     * share button, and shortcut back to the Organizer home screen.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_event_created, container, false);
    }

    /**
     * Pulls arguments out of the Bundle, generates the QR code, and hooks
     * up the share and navigation buttons once the view is ready.
     * Assumes the creator fragment passed a non empty event id.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            eventId = args.getString(ARG_EVENT_ID);
            eventTitle = args.getString(ARG_EVENT_TITLE);
            eventPosterUrl = args.getString(ARG_POSTER_URL);
            isPrivateEvent = args.getBoolean(ARG_IS_PRIVATE, false);
            isShareMode = args.getBoolean(ARG_IS_SHARE_MODE, false);
        }

        imgQrCode = view.findViewById(R.id.imgQrCode);
        Button btnShare = view.findViewById(R.id.btnShare);
        Button btnViewMyEvents = view.findViewById(R.id.btnViewMyEvents);

        View btnHamburger = view.findViewById(R.id.btnHamburger);
        if (btnHamburger != null) {
            btnHamburger.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        if (isShareMode) {
            TextView txtEventCreatedBanner = view.findViewById(R.id.txtEventCreatedBanner);
            if (txtEventCreatedBanner != null) {
                txtEventCreatedBanner.setText(R.string.organizer_share_event_title);
            }
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Missing event id", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isPrivateEvent) {
            generateQrCode(eventId);
        } else {
            imgQrCode.setImageDrawable(null);
            btnShare.setVisibility(View.GONE);
        }

        btnShare.setOnClickListener(v -> shareEvent());
        btnViewMyEvents.setOnClickListener(v -> navigateToMyEvents());
    }

    private void generateQrCode(@NonNull String content) {
        // I learned how to wire up this QR code generation flow by asking
        // ChatGPT to walk me through the ZXing usage step by step.
        QRCodeWriter writer = new QRCodeWriter();
        int sizePx = 800;
        try {
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            imgQrCode.setImageBitmap(bmp);
        } catch (WriterException e) {
            Toast.makeText(requireContext(), "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareEvent() {
        if (eventId == null) return;

        // Grab the currently rendered QR bitmap from the ImageView so we can
        // attach it to the share intent as an image
        imgQrCode.setDrawingCacheEnabled(true);
        imgQrCode.buildDrawingCache();
        Bitmap bitmap = imgQrCode.getDrawingCache();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        StringBuilder text = new StringBuilder("Event ID: " + eventId);
        if (eventTitle != null && !eventTitle.trim().isEmpty()) {
            text.append(" (").append(eventTitle).append(")");
        }
        shareIntent.putExtra(Intent.EXTRA_TEXT, text.toString());

        if (bitmap != null) {
            // Insert a temporary image into the MediaStore so other apps can
            // read it via content URI; this avoids FileProvider boilerplate.
            String path = MediaStore.Images.Media.insertImage(
                    requireContext().getContentResolver(), bitmap, "Event QR", null);
            if (path != null) {
                shareIntent.setType("image/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(path));
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }

        startActivity(Intent.createChooser(shareIntent, "Share event"));
    }

    private void navigateToMyEvents() {
        if (getActivity() instanceof OrganizerEntryActivity) {
            // Clear any organizer fragments that might be on the stack so
            // tapping back from My Events does not return to this screen.
            getParentFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            ((OrganizerEntryActivity) getActivity()).replaceWithOrganizerFragment(new OrganizerHomeFragment());
        }
    }
}

