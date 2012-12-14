/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.crumb.adv;

import java.util.HashMap;
import java.util.Map;
import jp.crumb.CrumbContext;
import jp.crumb.base.BulletInfo;
import jp.crumb.utils.Copy;
import jp.crumb.utils.Util;

/**
 *
 * @author crumb
 */
public class AdbCrumbContext extends CrumbContext{
    public Map<String,BulletInfo> nextEnemyBulletList = new HashMap<>();


    public AdbCrumbContext() {
    }
    public AdbCrumbContext(AdbCrumbContext in) {
        super(in);
        this.nextEnemyBulletList = Util.deepCopyHashMap(in.nextEnemyBulletList,new Copy<BulletInfo>(){
            @Override
            public BulletInfo copy(BulletInfo e) {
                return new BulletInfo(e);
            }
        });
    }

}
