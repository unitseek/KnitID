"""MkDocs build hooks for the versioned documentation site."""

import os
import shutil


def on_post_build(config, **kwargs):
    """Copy aggregated Javadoc into the generated site when available."""
    source = os.path.join(os.getcwd(), "target", "reports", "apidocs")
    destination = os.path.join(config["site_dir"], "apidocs")
    if not os.path.isdir(source):
        return
    if os.path.isdir(destination):
        shutil.rmtree(destination)
    shutil.copytree(source, destination)
