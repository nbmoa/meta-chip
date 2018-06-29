DESCRIPTION = "Linux Kernel for C.H.I.P. boards"
SECTION = "kernel"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"

COMPATIBLE_MACHINE = "chip"

inherit kernel

LINUX_VERSION ?= "4.4.13-ntc-mlc"
PV = "${LINUX_VERSION}+git${SRCPV}"

SRCREV ?= "ae9469864b5bda4ce08d65ceec58a2d256563c7b"
BRANCH ?= "debian/4.4.13-ntc-mlc"
SRC_URI = "git://github.com/NextThingCo/CHIP-linux.git;protocol=git;branch=${BRANCH} \
           file://0001-ubi-Add-null-definition-of-ubi_conso_remove_full_leb.patch \
           file://defconfig \
          "

S = "${WORKDIR}/git"

do_configure_append() {
        # Check for kernel config fragments.  The assumption is that the config
        # fragment will be specified with the absolute path.  For example:
        #   * ${WORKDIR}/config1.cfg
        #   * ${S}/config2.cfg
        # Iterate through the list of configs and make sure that you can find
        # each one.  If not then error out.
        # NOTE: If you want to override a configuration that is kept in the kernel
        #       with one from the OE meta data then you should make sure that the
        #       OE meta data version (i.e. ${WORKDIR}/config1.cfg) is listed
        #       after the in kernel configuration fragment.
        # Check if any config fragments are specified.
        if [ ! -z "${KERNEL_CONFIG_FRAGMENTS}" ]
        then
            for f in ${KERNEL_CONFIG_FRAGMENTS}
            do  
                # Check if the config fragment was copied into the WORKDIR from
                # the OE meta data
                if [ ! -e "$f" ]
                then
                    echo "Could not find kernel config fragment $f"
                    exit 1
                fi
            done

            # Now that all the fragments are located merge them.
            ( cd ${WORKDIR} && ${S}/scripts/kconfig/merge_config.sh -m -r -O ${B} ${B}/.config ${KERNEL_CONFIG_FRAGMENTS} 1>&2 )
        fi

	cp ${WORKDIR}/defconfig ${B}/.config

	yes '' | oe_runmake -C ${S} O=${B} oldconfig
	oe_runmake -C ${S} O=${B} savedefconfig && cp ${B}/defconfig ${WORKDIR}/defconfig.saved
}

# Automatically depend on lzop-native if CONFIG_KERNEL_LZO is enabled
python () {
    try:
        defconfig = bb.fetch2.localpath('file://defconfig', d)
    except bb.fetch2.FetchError:
        return

    try:
        configfile = open(defconfig)
    except IOError:
        return

    if 'CONFIG_KERNEL_LZO=y\n' in configfile.readlines():
        depends = d.getVar('DEPENDS', False)
        d.setVar('DEPENDS', depends + ' lzop-native')

    configfile.close()
}
