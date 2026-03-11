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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.waitwell.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Organizer-only confirmation screen shown after successfully creating an event.
 * User story: US 02.01.01 – Create Event & QR.
 * Purpose:
 *  - Generate and display a QR code for the created event (QR content is the eventId).
 *  - Allow organizer to share the event via Android share intents.
 *  - Allow quick navigation back to "My Events" (OrganizerHomeFragment).
 * Navigation:
 *  - This fragment is launched only from OrganizerCreateEventFragment after a successful Firestore write.
 *  - "View My Events" replaces the current fragment stack with OrganizerHomeFragment.
 */
public class OrganizerEventCreatedFragment extends Fragment {

    private static final String ARG_EVENT_ID = "event_id";
    private static final String ARG_EVENT_TITLE = "event_title";
    private static final String ARG_POSTER_URL = "event_poster_url";

    private String eventId;
    private String eventTitle;
    private String eventPosterUrl;

    private ImageView imgQrCode;

    public static OrganizerEventCreatedFragment newInstance(@NonNull String eventId,
                                                            @NonNull String eventTitle,
                                                            @Nullable String posterUrl) {
        OrganizerEventCreatedFragment fragment = new OrganizerEventCreatedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putString(ARG_EVENT_TITLE, eventTitle);
        args.putString(ARG_POSTER_URL, posterUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_event_created, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            eventId = args.getString(ARG_EVENT_ID);
            eventTitle = args.getString(ARG_EVENT_TITLE);
            eventPosterUrl = args.getString(ARG_POSTER_URL);
        }

        imgQrCode = view.findViewById(R.id.imgQrCode);
        Button btnShare = view.findViewById(R.id.btnShare);
        Button btnViewMyEvents = view.findViewById(R.id.btnViewMyEvents);

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Missing event id", Toast.LENGTH_SHORT).show();
            return;
        }

        generateQrCode(eventId);

        btnShare.setOnClickListener(v -> shareEvent());
        btnViewMyEvents.setOnClickListener(v -> navigateToMyEvents());
    }

    private void generateQrCode(@NonNull String content) {
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
            getParentFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            ((OrganizerEntryActivity) getActivity()).replaceWithOrganizerFragment(new OrganizerHomeFragment());
        }
    }
}

