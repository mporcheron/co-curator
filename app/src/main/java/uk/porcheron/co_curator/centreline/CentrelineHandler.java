package uk.porcheron.co_curator.centreline;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

import java.util.List;

import uk.porcheron.co_curator.user.User;
import uk.porcheron.co_curator.user.UserList;
import uk.porcheron.co_curator.util.Style;

/**
 * Created by map on 08/08/15.
 */
public class CentrelineHandler implements SurfaceHolder.Callback {

    private UserList mUsers;

    public CentrelineHandler(UserList users) {
        mUsers = users;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Canvas canvas = holder.lockCanvas();
        canvas.drawColor(Style.backgroundColour);

        int w = canvas.getWidth();
        int h = canvas.getHeight();
        for(User user : mUsers){
            int y1 = (int) ((h / 2) - (Style.lineWidth / 2) + user.offset);
            int y2 = (int) (y1 + Style.lineWidth);

            canvas.drawRect(0, y1, w, y2, user.paint);
        }

        holder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}