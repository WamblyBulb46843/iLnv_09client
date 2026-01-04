package dev.iLnv_09.core.impl;

import dev.iLnv_09.core.Manager;
import dev.iLnv_09.iLnv_09;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FriendManager extends Manager {
    public FriendManager() {
        read();
        addDefaultFriendsOnStart();
        save();
    }

    public final ArrayList<String> friendList = new ArrayList<>();
    public boolean isFriend(String name) {
        return friendList.contains(name);
    }
    public void removeFriend(String name) {
        friendList.remove(name);
    }
    public void addFriend(String name) {
        if (!friendList.contains(name)) {
            friendList.add(name);
        }
    }

    public void friend(PlayerEntity entity) {
        friend(entity.getGameProfile().getName());
    }

    private void addDefaultFriendsOnStart() {
        String defaultFriend1 = "iLnv_09"; // 第一个默认好友名
        String defaultFriend2 = "WamblyBulb46843"; // 第二个默认好友名

        if (!friendList.contains(defaultFriend1)) {
            friendList.add(defaultFriend1);
        }
        if (!friendList.contains(defaultFriend2)) {
            friendList.add(defaultFriend2);
        }
    }

    public void friend(String name) {
        // 假设 fixedFriends 是固定存在的好友集合
        List<String> fixedFriends = List.of("iLnv_09", "WamblyBulb46843"); // 你可以改成实际固定好友列表

        if (friendList.contains(name)) {
            if (fixedFriends.contains(name)) {
                // 固定好友，跳过删除操作
                return;
            }
            friendList.remove(name);
        } else {
            friendList.add(name);
        }
    }

    public void read() {
        try {
            File friendFile = getFile("friends.txt");
            if (!friendFile.exists())
                return;
            List<String> list = IOUtils.readLines(new FileInputStream(friendFile), StandardCharsets.UTF_8);

            for (String s : list) {
                addFriend(s);
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
    
    public void save() {
        try {
            File friendFile = getFile("friends.txt");
            PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(friendFile), StandardCharsets.UTF_8));
            for (String str : friendList) {
                printwriter.println(str);
            }
            printwriter.close();
        } catch (Exception exception) {
            System.out.println("[" + iLnv_09.NAME + "] Failed to save friends");
        }
    }
    

    public boolean isFriend(PlayerEntity entity) {
        return isFriend(entity.getGameProfile().getName());
    }
}
