package de.hauke_stieler.geonotes.note_list;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import de.hauke_stieler.geonotes.R;
import de.hauke_stieler.geonotes.notes.Note;
import de.hauke_stieler.geonotes.categories.Category;
import de.hauke_stieler.geonotes.notes.NoteIconProvider;

import static org.mockito.ArgumentMatchers.any;

public class NoteListAdapterTest {

    private NoteListAdapter adapter;

    private Context context;
    private NoteIconProvider noteIconProvider;
    private Resources resourcesMock;
    private ViewGroup layoutViewMock;
    private ImageView imageViewMock;
    private TextView textViewMock;
    private List<Note> notes;
    private List<Note> notesWithPhotos;
    private NoteListAdapter.NoteListClickListener clickListenerMock;
    private LayoutInflater inflater;

    @Before
    public void setup() {
        layoutViewMock = Mockito.mock(ViewGroup.class);

        inflater = Mockito.mock(LayoutInflater.class);
        Mockito.when(inflater.inflate(R.layout.note_list_row, null)).thenReturn(layoutViewMock);

        context = Mockito.mock(Context.class);
        Mockito.when(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).thenReturn(inflater);

        noteIconProvider = Mockito.mock(NoteIconProvider.class);

        resourcesMock = Mockito.mock(Resources.class);
        Mockito.when(context.getResources()).thenReturn(resourcesMock);

        imageViewMock = Mockito.mock(ImageView.class);
        Mockito.when(layoutViewMock.findViewById(R.id.note_list_row_icon)).thenReturn(imageViewMock);

        textViewMock = Mockito.mock(TextView.class);
        Mockito.when(layoutViewMock.findViewById(R.id.note_list_row_text_view)).thenReturn(textViewMock);

        notes = new ArrayList<>();
        notes.add(new Note(123L, "foo", 12, 23, "now", new Category(1, "", "", 1)));
        notes.add(new Note(234L, "bar", 34, 45, "i don't remember", new Category(1, "", "", 1)));
        notes.add(new Note(345L, "", 56, 56, "tomorrow", new Category(1, "", "", 1)));

        notesWithPhotos = new ArrayList<>();
        notesWithPhotos.add(notes.get(1));
        notesWithPhotos.add(notes.get(2));

        clickListenerMock = Mockito.mock(NoteListAdapter.NoteListClickListener.class);

        adapter = new NoteListAdapter(context, noteIconProvider, notes, notesWithPhotos, clickListenerMock);
    }

    @Test
    public void testGetCount() {
        // Act
        int count = adapter.getCount();

        // Assert
        Assert.assertEquals(notes.size(), count);
    }

    @Test
    public void testGetItems() {
        // Act & Assert
        for (int i = 0; i < notes.size(); i++) {
            Note item = adapter.getItem(i);

            Assert.assertEquals(notes.get(i), item);
        }
    }

    @Test
    public void testGetItemIds() {
        // Act & Assert
        for (int i = 0; i < notes.size(); i++) {
            long itemId = adapter.getItemId(i);

            Assert.assertEquals(notes.get(i).getId(), itemId);
        }
    }

    @Test
    public void testCreatingView_withoutPhoto() {
        // Act
        View view = adapter.getView(0, null, null);

        // Assert
        Assert.assertEquals(layoutViewMock, view);
        Mockito.verify(imageViewMock).setImageDrawable(any());
        Mockito.verifyNoMoreInteractions(imageViewMock);
        Mockito.verify(textViewMock).setText(notes.get(0).getDescription());
        Mockito.verify(layoutViewMock).setOnClickListener(any());
        Mockito.verifyNoMoreInteractions(textViewMock);
    }

    @Test
    public void testCreatingView_withPhoto() {
        // Act
        View view = adapter.getView(1, null, null);

        // Assert
        Assert.assertEquals(layoutViewMock, view);
        Mockito.verify(imageViewMock).setImageDrawable(any());
        Mockito.verifyNoMoreInteractions(imageViewMock);
        Mockito.verify(textViewMock).setText(notes.get(1).getDescription());
        Mockito.verify(layoutViewMock).setOnClickListener(any());
        Mockito.verifyNoMoreInteractions(textViewMock);
    }

    @Test
    public void testCreatingView_withPhotoOnly() {
        // Arrange
        int colorCode = 123;
        Mockito.when(resourcesMock.getColor(R.color.grey)).thenReturn(colorCode);

        // Act
        View view = adapter.getView(2, null, null);

        // Assert
        Assert.assertEquals(layoutViewMock, view);
        Mockito.verify(imageViewMock).setImageDrawable(any());
        Mockito.verifyNoMoreInteractions(imageViewMock);
        Mockito.verify(textViewMock).setText("(only photo)");
        Mockito.verify(textViewMock).setTypeface(null, Typeface.ITALIC);
        Mockito.verify(textViewMock).setTextColor(colorCode);
        Mockito.verify(layoutViewMock).setOnClickListener(any());
        Mockito.verifyNoMoreInteractions(textViewMock);
    }

    @Test
    public void testClickOnTextView() {
        // Arrange
        int noteIndex = 1;
        adapter.getView(noteIndex, null, null);

        ArgumentCaptor<View.OnClickListener> clickListenerArgumentCaptor = ArgumentCaptor.forClass(View.OnClickListener.class);
        Mockito.verify(layoutViewMock).setOnClickListener(clickListenerArgumentCaptor.capture());

        // Act
        clickListenerArgumentCaptor.getValue().onClick(textViewMock); // simulate the click by manually executing event listener

        // Assert
        Mockito.verify(clickListenerMock).onClick(notes.get(noteIndex).getId());
    }
}
