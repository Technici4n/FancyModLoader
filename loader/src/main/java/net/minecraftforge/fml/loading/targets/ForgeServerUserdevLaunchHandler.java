/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fml.loading.targets;

import net.minecraftforge.api.distmarker.Dist;

public class ForgeServerUserdevLaunchHandler extends ForgeUserdevLaunchHandler {
    @Override public String name() { return "forgeserveruserdev"; }
    @Override public Dist getDist() { return Dist.DEDICATED_SERVER; }

    @Override
    public void runService(String[] arguments, ModuleLayer layer) throws Throwable {
        serverService(arguments, layer);
    }
}