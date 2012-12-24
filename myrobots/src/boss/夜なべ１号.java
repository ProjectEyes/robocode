package boss;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */





/**
 *
 * @author crumb
 */
public class 夜なべ１号 extends Boss {

    @Override
    protected void cbFirst() {
        super.cbFirst();
        setFireMode(ctx.MODE_FIRE_AUTO);
    }

}
